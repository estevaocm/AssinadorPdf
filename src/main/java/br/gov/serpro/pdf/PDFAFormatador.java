/*
 * AssinadorPdf
 * Copyright (C) 2018 SERPRO
 * ----------------------------------------------------------------------------
 * RubricaClient is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License version 3
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this program; if not,  see <http://www.gnu.org/licenses/>
 * or write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA  02110-1301, USA.
 * ----------------------------------------------------------------------------
 * RubricaClient é um software livre; você pode redistribuí-lo e/ou
 * modificá-lo dentro dos termos da GNU LGPL versão 3 publicada pela Fundação
 * do Software Livre (FSF).
 *
 * Este programa é distribuído com a esperança que possa ser útil, mas SEM GARANTIA
 * ALGUMA; sem sequer uma garantia implícita de ADEQUAÇÃO a qualquer MERCADO ou
 * APLICAÇÃO ESPECÍFICA. Veja a Licença Pública Geral GNU/LGPL em português
 * para mais detalhes.
 *
 * Você deve ter recebido uma cópia da GNU LGPL versão 3, sob o título
 * "LICENCA.txt", junto com esse programa. Caso contrário, acesse 
 * <http://www.gnu.org/licenses/> ou escreva para a Fundação do Software Livre (FSF) 
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02111-1301, USA.
 */
package br.gov.serpro.pdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;

/**
 * 
 * @author Estêvão Monteiro
 * @since 15/02/2018
 * @see https://pdfbox.apache.org/1.8/cookbook/pdfacreation.html
 *
 */
public class PDFAFormatador {
	
	public static final String SRGB_INFO = "sRGB IEC61966-2.1";
	public static final String SRGB_REG = "http://www.color.org";
	public static final String SRGB_ICC = "/pdfa/color/sRGB.icc";
	private static final Logger L = Logger.getLogger(PDFAFormatador.class);
	
	private PDDocument doc;
	
	public PDFAFormatador(File input) throws IOException, InvalidPasswordException {
		this.doc = PDDocument.load(input);
	}

	/**
	 * 
	 * @param in
	 * @param output
	 * @throws IOException
	 * @throws TransformerException
	 * @see https://stackoverflow.com/questions/33607652/converting-pdf-to-pdf-a-with-pdfbox
	 */
	public void formatar(File output) throws IOException, TransformerException {
        try{
        	//TODO detectar os erros antes de corrigi-los
        	//getDocInfo(doc);
        	identificarFontes();
    		//PDType1Font Helvetica
    		//PDType1Font Helvetica-Bold
        	embutirFonte(PDFAFormatador.class.getResourceAsStream("/pdfa/fonts/Helvetica.ttf"));
        	embutirFonte(PDFAFormatador.class.getResourceAsStream("/pdfa/fonts/Helvetica-Bold.ttf"));
        	definirMetadadosXMP();
            definirEsquemaCoresRGB();
            this.doc.save(output);
        }
        finally{
        	this.doc.close();
        }
	}
	
	public Collection<PDFont> identificarFontes() throws IOException {
		Collection<PDFont> fontes = new ArrayList<PDFont>();
    	for (int i = 0; i < this.doc.getNumberOfPages(); ++i){
    	    PDPage page = this.doc.getPage(i);
    	    PDResources res = page.getResources();
    	    for (COSName fontName : res.getFontNames()){
    	        PDFont font = res.getFont(fontName);
    	        fontes.add(font);
    	        L.info(font);
    	    }
    	}
    	return fontes;
	}
	
	/**
	 * 
	 * @param doc
	 * @param fontfile
	 * @throws IOException
	 * @see https://pdfbox.apache.org/1.8/cookbook/workingwithfonts.html
	 */
	public void embutirFonte(InputStream fontfile) throws IOException {
        // load the font as this needs to be embedded
		if(fontfile == null) {
			throw new IllegalArgumentException("fontfile=null");
		}
		
        PDFont font = PDType0Font.load(this.doc, fontfile);
        //PDFont font = new PDType1Font(this.doc, fontfile);
        
        // A PDF/A file needs to have the font embedded if the font is used for text rendering
        // in rendering modes other than text rendering mode 3.
        //
        // This requirement includes the PDF standard fonts, so don't use their static PDFType1Font classes such as
        // PDFType1Font.HELVETICA.
        //
        // As there are many different font licenses it is up to the developer to check if the license terms for the
        // font loaded allows embedding in the PDF.
        // 
        if (!font.isEmbedded()){
        	throw new IllegalStateException("PDF/A compliance requires that all fonts used for"
        			+ " text rendering in rendering modes other than rendering mode 3 are embedded.");
        }
	}
	
	public void definirMetadadosXMP() throws IOException, TransformerException {
        PDDocumentInformation info = getDocInfo();
        String titulo = info.getTitle();
        titulo = titulo == null ? "" : titulo;
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        try{
            DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
            dc.setTitle(titulo);
            dc.addCreator(info.getCreator());
            
            AdobePDFSchema adobePDFSchema = xmp.createAndAddAdobePDFSchema();
            adobePDFSchema.setProducer(info.getProducer());
            
            XMPBasicSchema xmpBasicSchema = xmp.createAndAddXMPBasicSchema();
            xmpBasicSchema.setCreatorTool(info.getCreator());
            xmpBasicSchema.setCreateDate(info.getCreationDate());
            xmpBasicSchema.setModifyDate(info.getModificationDate());
            
            PDFAIdentificationSchema id = xmp.createAndAddPFAIdentificationSchema();
            id.setPart(1);
            id.setConformance("B");
            xmp.addSchema(id);
            
            XmpSerializer serializer = new XmpSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(xmp, baos, true);

            PDMetadata metadata = new PDMetadata(this.doc);
            metadata.importXMPMetadata(baos.toByteArray());
            this.doc.getDocumentCatalog().setMetadata(metadata);
        }
        catch(BadFieldValueException e){
            // won't happen here, as the provided value is valid
            throw new IllegalArgumentException(e.getMessage(), e);
        }		
	}
	
	public void definirEsquemaCores(InputStream colorProfile, String info, String outputCondition, 
			String outputConditionIdentifier, String registryName) throws IOException {
		
		if(colorProfile == null) {
			throw new IllegalArgumentException("colorProfile=null");
		}
        PDOutputIntent intent = new PDOutputIntent(this.doc, colorProfile);
        intent.setInfo(info);
        intent.setOutputCondition(outputCondition);
        intent.setOutputConditionIdentifier(outputConditionIdentifier);
        intent.setRegistryName(registryName);
        this.doc.getDocumentCatalog().addOutputIntent(intent);		
	}

	public void definirEsquemaCores(File arq, String info, String outputCondition, 
			String outputConditionIdentifier, String registryName) throws IOException {
		
		definirEsquemaCores(new FileInputStream(arq), info, outputCondition, outputConditionIdentifier, registryName);
	}
	
	public void definirEsquemaCoresRGB() throws IOException {
		InputStream in = PDFAFormatador.class.getResourceAsStream(SRGB_ICC);
		definirEsquemaCores(in, SRGB_INFO, SRGB_INFO, SRGB_INFO, SRGB_REG);
	}
	
	public PDDocumentInformation getDocInfo() {
    	PDDocumentInformation info = this.doc.getDocumentInformation();
    	L.info("Page Count=" + doc.getNumberOfPages());
    	L.info("Title=" + info.getTitle());
    	L.info("Author=" + info.getAuthor());
    	L.info("Subject=" + info.getSubject());
    	L.info("Keywords=" + info.getKeywords());
    	L.info("Creator=" + info.getCreator());
    	L.info("Producer=" + info.getProducer());
    	L.info("Creation Date=" + info.getCreationDate());
    	L.info("Modification Date=" + info.getModificationDate());
    	L.info("Trapped=" + info.getTrapped());
    	/*
        Page Count=1
        Title=null
        Author=null
        Subject=null
        Keywords=null
        Creator=JasperReports (GerarRelatorioEmitirOficioPagamento)
        Producer=iText1.3.1 by lowagie.com (based on itext-paulo-154)
        Creation Date=java.util.GregorianCalendar[time=1508339263000,areFieldsSet=true,areAllFieldsSet=true,lenient=false,zone=java.util.SimpleTimeZone[id=GMT-03:00,offset=-10800000,dstSavings=3600000,useDaylight=false,startYear=0,startMode=0,startMonth=0,startDay=0,startDayOfWeek=0,startTime=0,startTimeMode=0,endMode=0,endMonth=0,endDay=0,endDayOfWeek=0,endTime=0,endTimeMode=0],firstDayOfWeek=1,minimalDaysInFirstWeek=1,ERA=1,YEAR=2017,MONTH=9,WEEK_OF_YEAR=42,WEEK_OF_MONTH=3,DAY_OF_MONTH=18,DAY_OF_YEAR=291,DAY_OF_WEEK=4,DAY_OF_WEEK_IN_MONTH=3,AM_PM=1,HOUR=0,HOUR_OF_DAY=12,MINUTE=7,SECOND=43,MILLISECOND=0,ZONE_OFFSET=-10800000,DST_OFFSET=0]
        Modification Date=java.util.GregorianCalendar[time=1508339263000,areFieldsSet=true,areAllFieldsSet=true,lenient=false,zone=java.util.SimpleTimeZone[id=GMT-03:00,offset=-10800000,dstSavings=3600000,useDaylight=false,startYear=0,startMode=0,startMonth=0,startDay=0,startDayOfWeek=0,startTime=0,startTimeMode=0,endMode=0,endMonth=0,endDay=0,endDayOfWeek=0,endTime=0,endTimeMode=0],firstDayOfWeek=1,minimalDaysInFirstWeek=1,ERA=1,YEAR=2017,MONTH=9,WEEK_OF_YEAR=42,WEEK_OF_MONTH=3,DAY_OF_MONTH=18,DAY_OF_YEAR=291,DAY_OF_WEEK=4,DAY_OF_WEEK_IN_MONTH=3,AM_PM=1,HOUR=0,HOUR_OF_DAY=12,MINUTE=7,SECOND=43,MILLISECOND=0,ZONE_OFFSET=-10800000,DST_OFFSET=0]
        Trapped=null        	 
        */
		return info;
	}
	
}
