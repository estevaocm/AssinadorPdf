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
package br.gov.serpro.pdf.assinador.util;

import java.io.File;
import java.io.IOException;

import gui.ava.html.image.generator.HtmlImageGenerator;

/**
 * 
 * @author Fábio Araújo, Estêvão Monteiro
 *
 */
public class EstampaUtil {
	
	public static File gerarEstampa(String organização, String nome, String cpf, String cargo, String data, String link)
			throws IOException {
				
		String html = "<!DOCTYPE html>"
				+ "<html lang=\"pt-br\">"
					+ "<head>"
					+ "<meta charset=\"utf-8\">"
						+ "<style>"
						+ ".principal {"
						+ "border: solid 1px;"
						+ "margin: 0;"
						+ "padding: 10px;"
						+ "width: 350px;"
						+ "font-size: 12px;"
						+ "}"												
						+ ".titulo {"
						+ "font-weight: bold; "
						+ "text-align: center;"
						+ "padding-bottom: 5px;"
						+ "font-size: 13px;"
						+ "}"
						+ ".label {"
						+ "padding-right: 5px;"
						+ "font-weight: bold;"
						+ "} "
						+ ".center {"
						+ "text-align: center;"
						+ "}"
						+ "p {"
						+ "text-align: center;"
						+ "padding: 0px;"
						+ "margin: 0px;"
						+ "}"						
						+ "</style>"
					+ "</head>"
					+ "<body style=\"text-align: center\">"
						+ "<div class=\"principal\">"
							+ "<div>"
								+ "<div class=\"titulo\">"+organização+"</div>"
								+ "<hr />"
								+ "<div>"
									+ "<span>Documento assinado digitalmente em "+data+" por: </span>"
								+ "</div>"
								+ "<div class=\"label\">"+nome+"</div>"
								+ "<div>"+cargo+"</div>"
								+ "<div>"
									+ "<span>CPF "+cpf+"</span>"
								+ "</div>"
								+ "<br />"
								+ "<div>"
									+ "<p>Verifique a autenticidade do documento no endere&ccedil;o: </p>"
									+ "<p>" + link + "</p>"
								+ "</div>"
								+ "</div>"
						+ "</div>"
					+ "</body>"
				+ "</html>";

	  HtmlImageGenerator imageGenerator = new HtmlImageGenerator();
	  imageGenerator.loadHtml(html);
	  File img = File.createTempFile("estampa", ".png");
      imageGenerator.saveAsImage(img);
      return img;
	}
}
