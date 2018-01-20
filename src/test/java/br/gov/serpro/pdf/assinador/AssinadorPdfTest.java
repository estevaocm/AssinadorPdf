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
package br.gov.serpro.pdf.assinador;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;

import br.gov.serpro.pdf.assinador.util.EstampaUtil;

/**
 * 
 * @author Estêvão Monteiro
 * @since 23/10/17
 */
public class AssinadorPdfTest {

	private static final SimpleDateFormat FORMATO_DATA_ASSINATURA = new SimpleDateFormat("dd-MM-yyyy HH:mm");
	
	public static void main(String[] args) throws Exception{
		try{
			File in = new File("./src/test/resources/ofpg.pdf");
			File out = new File("./src/test/resources/ofpg-signed-stamp.pdf");
			
			Calendar data = Calendar.getInstance();
			data.setTime(FORMATO_DATA_ASSINATURA.parse("19-01-2018 14:05"));
			
			AssinadorPdf assinadorPdf = new AssinadorPdf(in, out, data);
			//AssinadorPdf assinadorPdf = new AssinadorPdf(FileUtils.readFileToByteArray(in), out);
			//testarAssinaturaSincrona(assinadorPdf);
			//testarAssinaturaAssincrona(assinadorPdf);
			testarAssinaturaAssincrona2(assinadorPdf);
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}
	
	private static void testarAssinaturaSincrona(AssinadorPdf assinadorPdf) throws Throwable{
		assinadorPdf.sign();
	}
	
	private static void testarAssinaturaAssincrona(AssinadorPdf assinadorPdf) throws Throwable{
		File imagem = EstampaUtil.gerarEstampa(
				"Organização",
				"Fulano", 
				"999.999.999-99", 
				"COORDENADOR DE LICITACOES DE SERVIÇOS ADM. E AQUISICOES DE BENS E CONTRATOS", 
				"08/01/2018 17:15", "http://assinador.serpro.gov.br/validacao");
		
		assinadorPdf.prepararEstampa(new FileInputStream(imagem), 10, 700, -40);
		byte[] hash = assinadorPdf.hash();//backend calcula o hash
		//byte[] preparado = assinadorPdf.getConteudo();
		
		byte[] assinatura = new AssinadorPdfToken().signHash(hash);//frontend assina o hash
		if(assinatura == null) return;
		assinadorPdf.sign(assinatura);//backend assina o PDF com assinatura recebida do frontend
		
		assinadorPdf.close();
	}	

	private static void testarAssinaturaAssincrona2(AssinadorPdf assinadorPdf) throws Throwable{
		File imagem = new File("~/estampa4450810727878248742.png");
		
		assinadorPdf.prepararEstampa(new FileInputStream(imagem), 30, 690, -50);
		byte[] hash = assinadorPdf.hash();//backend calcula o hash
		//byte[] preparado = assinadorPdf.getConteudo();
		
		byte[] assinatura = new AssinadorPdfToken().signHash(hash);//frontend assina o hash
		System.out.println(new String(Base64.getEncoder().encodeToString(assinatura)));
		if(assinatura == null) return;
		assinadorPdf.sign(assinatura);//backend assina o PDF com assinatura recebida do frontend
		
		assinadorPdf.close();
	}	
}