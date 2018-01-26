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