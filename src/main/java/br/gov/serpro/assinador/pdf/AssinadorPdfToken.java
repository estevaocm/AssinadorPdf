package br.gov.serpro.assinador.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.gov.serpro.assinador.AssinadorToken;

/**
 * Implementa a interface org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface
 * para assinar um documento PDF com uma chave privada proveniente de token.
 * @author Estêvão Monteiro
 * @since 23/10/17
 */
public class AssinadorPdfToken extends AssinadorToken implements SignatureInterface{
	
	private static final Logger L = LoggerFactory.getLogger(AssinadorPdfToken.class);
	
	public AssinadorPdfToken() throws IOException, GeneralSecurityException{
		super();
	}
	
	/*
	 * (non-Javadoc)
	 * @see br.gov.serpro.pdf.assinador.AssinadorToken#sign(java.io.InputStream)
	 */
	public byte[] sign(InputStream is) {
		//SequenceInputStream
		byte[] content = null;
		try {
			try (ByteArrayOutputStream out = new ByteArrayOutputStream();){
				IOUtils.copy(is, out);
				content = out.toByteArray();
			}
		} 
		catch (Throwable t) {
			L.info("Algo falhou: " + t.getMessage() + (t.getCause()!=null?
					" Causa: "+t.getCause().getMessage():". sem causa."));
			t.printStackTrace();
			return null;
		}
		return super.sign(content);
	}

}