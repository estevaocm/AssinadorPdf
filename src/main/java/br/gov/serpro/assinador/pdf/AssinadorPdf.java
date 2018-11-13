package br.gov.serpro.assinador.pdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Estêvão Monteiro
 * @since 23/10/17
 */
public class AssinadorPdf {
	
	private static final Logger L = LoggerFactory.getLogger(AssinadorPdf.class);
	protected static final Long SEED = 182959L;
	
	//Parâmetros externos:
	protected Calendar data;
	protected File original;
	protected byte[] originalBytes;
	protected File assinado;
	protected String contato;
	protected String localidade;
	protected String motivo;
	
	//Objetos produzidos:
	protected byte[] hash;
	protected byte[] assinatura;
	protected ExternalSigningSupport externalSigningSupport;
	protected PDDocument doc;
	protected PDSignature assinaturaPdf;
	protected Estampa estampa;
	
	//Variáveis de controle lógico:
	protected boolean encerrado;
	
	public AssinadorPdf(File original, File assinado, Calendar data, String contato, String localidade, String motivo){
		if(original != null && !original.exists()){
			throw new IllegalArgumentException("O arquivo " + this.original + " não existe");
		}
		this.original = original;
		this.assinado = assinado;
		if(contato != null && !contato.trim().isEmpty()){
			this.contato = contato.trim();
		}
		if(localidade != null && !localidade.isEmpty()){
			this.localidade = localidade.trim();
		}
		if(motivo != null && !motivo.isEmpty()){
			this.motivo = motivo.trim();
		}
		if(data == null){
			this.data = Calendar.getInstance();
			//não é necessária a precisão de segundos, basta h:min.
			this.data.set(Calendar.SECOND, 0);
		}
		else{
			this.data = data;
		}
		makePDSignature();
	}
	
	public AssinadorPdf(byte[] originalBytes, File assinado, Calendar data, String contato, String localidade, 
			String motivo){
		this((File) null, assinado, data, contato, localidade, motivo);
		if(originalBytes == null){
			throw new NullPointerException("byte[] originalBytes");
		}
		this.originalBytes = originalBytes;
		if(assinado == null){
			throw new NullPointerException("File assinado");
		}
	}
	
	public AssinadorPdf(byte[] originalBytes, File assinado, Calendar data){
		this(originalBytes, assinado, data, null, null, null);
	}
	
	public AssinadorPdf(byte[] originalBytes, File assinado){
		this(originalBytes, assinado, null);
	}
	
	public AssinadorPdf(File original, File assinado, Calendar data){
		this(original, assinado, data, null, null, null);
	}
	
	public AssinadorPdf(File original, File assinado){
		this(original, assinado, null);
	}
	
	public AssinadorPdf(File original){
		this(original, null);
	}
	
	/**
	 * Assina um documento PDF em uma operação só, solicitando o PIN do token.
	 * @param original
	 * @param assinado
	 * @param contato
	 * @param localidade
	 * @param motivo
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public void sign() throws IOException, GeneralSecurityException {
		verificarEstado();
		//Obtém os dados a assinar:
		InputStream corpoAssinar = this.externalSigningSupport.getContent();
		this.assinatura =  new AssinadorPdfToken().sign(corpoAssinar);
		//Insere a assinatura CMS no documento:
		this.externalSigningSupport.setSignature(assinatura);
		close();
	}

	/**
	 * Embute a assinatura informada (prévia ou externamente gerada) no documento PDF.
	 * É importante conferir previamente o hash da assinatura com o deste objeto,
	 * atentando para o algoritmo (SHA-256, SHA-512 etc.). Se estiverem iguais,
	 * o PDF resultante ficará com a assinatura válida. 
	 * @param original
	 * @param assinado
	 * @param assinatura
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public void sign(byte[] assinatura) throws IOException, GeneralSecurityException {
		if(assinatura == null){
			throw new NullPointerException("assinatura nula");
		}
		verificarEstado();
		this.assinatura = assinatura;
		
		//Insere a assinatura CMS no documento:
		this.externalSigningSupport.setSignature(assinatura);
		close();
	}

	public byte[] hash(String algoritmo) throws IOException, GeneralSecurityException {
		if(this.hash != null){
			L.warn("Substituindo hash anterior.");
		}
		verificarEstado();
		byte[] content = getConteudo();
		this.hash = hash(content, algoritmo);
		return this.hash;
	}

	public static byte[] hash(byte[] content, String algoritmo) throws IOException, GeneralSecurityException {
		if(algoritmo == null || algoritmo.isEmpty()) {
			algoritmo = "SHA-256";
		}
		//alg = "SHA-512";
		MessageDigest md = MessageDigest.getInstance(algoritmo);
		md.update(content);
		//salvarTesteTxt(content);
		byte[] hash = md.digest();
		L.info(Base64.toBase64String(hash));
		return hash;
	}

	public void close() throws IOException{
		if(!this.encerrado){
			this.doc.close();
			this.encerrado = true;
		}
	}
	
	/**
	 * Exclui o arquivo produzido e fecha. Indicado no caso de assinatura assóncrona do hash.
	 * @throws IOException 
	 */
	public void cleanup() throws IOException{
		if(this.assinado.exists()){
			if(this.assinado.delete()){
				L.info(this.assinado + " excluído com sucesso.");
			}
		}
		close();
	}

	public boolean isClosed() {
		return this.encerrado;
	}

	protected void load() throws IOException, GeneralSecurityException{
		//TODO converter para PDF/A
		if(this.doc != null){
			this.doc.close();
		}
		
		if(this.original != null){
			this.doc = PDDocument.load(this.original);
		}
		else if(this.originalBytes != null){
			this.doc = PDDocument.load(this.originalBytes);
		}
		else{
			throw new IllegalStateException("Não há referência para um documento PDF");
		}

        int accessPermissions = SigUtils.getMDPPermission(this.doc);
        if (accessPermissions == 1){
            throw new IllegalStateException(
            		"não é permitido modificar o documento devido ao dicionário de Parâmetros de transformação DocMDP");
        }
        
        /*
        // Note that PDFBox has a bug that visual signing on certified files with permission 2
        // doesn't work properly, see PDFBOX-3699. As long as this issue is open, you may want to
        // be careful with such files.
        
        // Optional: certify
        // can be done only if version is at least 1.5 and if not already set
        // doing this on a PDF/A-1b file fails validation by Adobe preflight (PDFBOX-3821)
        // PDF/A-1b requires PDF version 1.4 max, so don't increase the version on such files.
        if (this.doc.getVersion() >= 1.5f && accessPermissions == 0)
        {
            SigUtils.setMDPPermission(this.doc, this.assinaturaPdf, 2);
        }

        PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
        if (acroForm != null && acroForm.getNeedAppearances())
        {
            // PDFBOX-3738 NeedAppearances true results in visible signature becoming invisible 
            // with Adobe Reader
            if (acroForm.getFields().isEmpty())
            {
                // we can safely delete it if there are no fields
                acroForm.getCOSObject().removeItem(COSName.NEED_APPEARANCES);
                // note that if you've set MDP permissions, the removal of this item
                // may result in Adobe Reader claiming that the document has been changed.
                // and/or that field content won't be displayed properly.
                // ==> decide what you prefer and adjust your code accordingly.
            }
            else
            {
                System.out.println("/NeedAppearances is set, signature may be ignored by Adobe Reader");
            }
        }
        */
    }
	
	protected void prepararAssinatura() throws IOException, GeneralSecurityException{
		//Define a propriedade "ID" no "trailer" do PDF para garantir que o hash não mude em execuções subsequentes:
		this.doc.setDocumentId(SEED);
		//Prepara objeto para receber a assinatura no documento:
		if(this.estampa == null){
			this.doc.addSignature(this.assinaturaPdf);
		}
		else{
			this.doc.addSignature(this.assinaturaPdf, this.estampa.buildOptions());
		}
		
		//Prepara PDF para ser assinado e Obtém classe auxiliar:
		if(this.assinado == null){
			this.assinado = getOutFile(this.original);
		}
		this.externalSigningSupport = this.doc.saveIncrementalForExternalSigning(new FileOutputStream(assinado));		
	}

	protected File getOutFile(File original) throws IOException{
		//File.createTempFile("toSign_", ".pdf")
		String assinado = original.getCanonicalPath();
		assinado = assinado.substring(0, assinado.lastIndexOf('.')) + "-signed.pdf";
		return new File(assinado);
	}
	
	public byte[] getConteudo() throws IOException, GeneralSecurityException {
		
		InputStream in = this.externalSigningSupport.getContent();
		//SequenceInputStream: org.apache.pdfbox.io.RandomAccessInputStream, org.apache.pdfbox.pdmodel.interactive.digitalsignature.COSFilterInputStream

		byte[] content = null;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();){
			IOUtils.copy(in, out);
			content = out.toByteArray();
		}
		in.close();
		if(content.length == 0){
			throw new IllegalStateException("Documento vazio");
		}
		return content;
	}

	//private PDSignature makePDSignature(String contato, String localidade, String motivo){
	protected void makePDSignature(){
		//CUIDADO: Se estes Parâmetros mudarem, o hash também mudará e invalidará a assinatura subsequente. 
		this.assinaturaPdf = new PDSignature();
		this.assinaturaPdf.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
		this.assinaturaPdf.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
		
		this.assinaturaPdf.setSignDate(this.data);
		
		if(this.contato != null){
			this.assinaturaPdf.setContactInfo(this.contato);
		}
		if(localidade != null){
			this.assinaturaPdf.setLocation(this.localidade);
		}
		if(motivo != null){
			this.assinaturaPdf.setReason(this.motivo);
		}
		//assinaturaPdf.setName();
		//assinaturaPdf.setType();
		//return assinaturaPdf;
	}

	protected void verificarEstado() throws IOException, GeneralSecurityException{
		if(this.encerrado){
			throw new IllegalStateException("objeto encerrado");
		}
		if(this.doc == null){
			load();
			prepararAssinatura();
		}
	}
	
	/**
	 * Recupera o PDDocument que está sendo trabalhado. Cuidado ao manipular este objeto para não deixar o
	 * AssinadorPdf em estado inválido. Se possível, usar apenas para consultas.
	 * @return
	 * @throws GeneralSecurityException 
	 * @throws IOException 
	 */
	public PDDocument getPDDocument() throws IOException, GeneralSecurityException {
		if(this.doc == null) {
			load();
		}
		return this.doc;
	}

	/*
	 * Método auxiliar para testes
	 */
	private void salvarTesteTxt(byte[] content) throws IOException{
		try (FileOutputStream out = new FileOutputStream(new File("teste.txt"));){
			out.write(content);
			out.flush();
		}
	}
	
	/**
	 * Prepara o documento para receber uma estampa de assinatura (opcional).
	 * Chamar este método logo após criar o AssinadorPdf e antes de hash() ou sign().
	 * @param imagem
	 * @param x
	 * @param y
	 * @param zoomPercent
	 * @param pagina
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public void prepararEstampa(InputStream imagem, int pagina, float x, float y, float zoomPercent)
			throws IOException, GeneralSecurityException{
		load();
		//Tem que ser chamado após AssinadorPdf() mas antes de hash()
		this.estampa = new Estampa(this.doc, imagem, pagina, x, y, zoomPercent, this.assinaturaPdf);
		prepararAssinatura();
	}
	
	protected static class Estampa{
		
		protected PDDocument doc;
		protected PDVisibleSignDesigner design;
		protected PDVisibleSigProperties props;
		protected SignatureOptions options;		
		
		protected Estampa(PDDocument doc, InputStream imagem, int pagina, float x, float y, float zoomPercent, 
				PDSignature descAssinatura) throws IOException{
			
			this.doc = doc;
			
			this.design = new PDVisibleSignDesigner(this.doc, imagem, pagina);
			this.design.xAxis(x)
				.yAxis(y)
				.zoom(zoomPercent)
				.adjustForRotation();
			
			//TODO verificar se realmente é necessário copiar de descAssinatura
			
			this.props = new PDVisibleSigProperties()
					.signerName(descAssinatura.getName())
					.signerLocation(descAssinatura.getLocation())
					.signatureReason(descAssinatura.getReason())
					.preferredSize(0) //default
					.page(pagina).visualSignEnabled(true) //se não fosse, não estaria criando Estampa
					.setPdVisibleSignature(this.design);
			
		}
		
		protected SignatureOptions buildOptions() throws IOException{
			this.props.buildSignature();
			this.options = new SignatureOptions();
			this.options.setVisualSignature(this.props.getVisibleSignature());
			this.options.setPage(this.props.getPage() - 1);
			return this.options;
		}
	}

}
