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

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.ValidationResult.ValidationError;
import org.apache.pdfbox.preflight.exception.SyntaxValidationException;
import org.apache.pdfbox.preflight.parser.PreflightParser;

/**
 * 
 * @author Estêvão Monteiro
 * @since 14/02/2018
 * @see https://pdfbox.apache.org/1.8/cookbook/pdfavalidation.html
 * https://pdfbox.apache.org/1.8/cookbook/pdfacreation.html
 */
public class PDFAValidador {
	
	private static final Logger L = Logger.getLogger(PDFAValidador.class);
	
	private File doc;
	private ValidationResult result = null;
	
	public PDFAValidador(File doc) {
		this.doc = doc;
	}

	public boolean verificarPDFA() throws IOException{
		PreflightDocument document = null;
		PreflightParser parser = new PreflightParser(this.doc);
		
		try{

		    /* Parse the PDF file with PreflightParser that inherits from the NonSequentialParser.
		     * Some additional controls are present to check a set of PDF/A requirements. 
		     * (Stream length consistency, EOL after some Keyword...)
		     */
		    parser.parse();

		    /* Once the syntax validation is done, 
		     * the parser can provide a PreflightDocument 
		     * (that inherits from PDDocument) 
		     * This document process the end of PDF/A validation.
		     */
		    document = parser.getPreflightDocument();
		    document.validate();

		    // Get validation result
		    this.result = document.getResult();

		}
		catch (SyntaxValidationException e){
		    /* the parse method can throw a SyntaxValidationException 
		     * if the PDF file can't be parsed.
		     * In this case, the exception contains an instance of ValidationResult  
		     */
		    this.result = e.getResult();
		}
		finally {
			if(document != null) {
				document.close();
			}
		}
		
		boolean valido = this.result.isValid();

		// display validation result
		if (valido){
			L.info("The file " + this.doc + " is a valid PDF/A-1b file");
		}
		else{
			L.warn("The file" + this.doc + " is not valid, error(s) :");
		    for (ValidationError error : this.result.getErrorsList()){
		    	L.warn(error.getErrorCode() + " : " + error.getDetails());
		    }
		}
		return valido;
	}

	public File getDoc() {
		return doc;
	}

	public void setDoc(File doc) {
		this.doc = doc;
		this.result = null;
	}

	public ValidationResult getResult() {
		return result;
	}
}