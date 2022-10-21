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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.bouncycastle.util.encoders.Base64;
import org.demoiselle.signer.policy.impl.cades.SignerAlgorithmEnum;

import br.gov.serpro.assinador.pdf.AssinadorPdf;
import br.gov.serpro.assinador.pdf.AssinadorPdfToken;
import br.gov.serpro.pdf.assinador.util.EstampaUtil;

/**
 *
 * @author Estêvão Monteiro
 * @since 23/10/17
 */
public class AssinadorPdfTest {

    private static final SimpleDateFormat FORMATO_DATA_ASSINATURA = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    public static void main(String[] args) throws Exception {
        try {
            File in = new File("./src/test/resources/sheet.pdf");
            File out = new File("./src/test/resources/sheet-signed-stamp.pdf");

            Calendar data = Calendar.getInstance();
            data.setTime(FORMATO_DATA_ASSINATURA.parse("08-11-2022 19:10"));

            AssinadorPdf assinadorPdf = new AssinadorPdf(in, out, data);
            // assinadorPdf.sign();
            testarAssinaturaAssincrona(assinadorPdf);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void testarAssinaturaAssincrona(AssinadorPdf assinadorPdf) throws Throwable {
        File imagem = recuperarImagem();
        imagem = gerarImagem();

        int pagina = assinadorPdf.getPDDocument().getNumberOfPages();
        assinadorPdf.prepararEstampa(new FileInputStream(imagem), pagina, 30, 690, -50);
        byte[] hash = assinadorPdf.hash("SHA-512");// backend calcula o hash
        // byte[] preparado = assinadorPdf.getConteudo();

        byte[] assinatura = new AssinadorPdfToken().signHash(hash);// frontend assina o hash
        if (assinatura == null) {
            return;
        }
        System.out.println(Base64.toBase64String(assinatura));

        // new AssinadorPdfToken().validarHash(hash, assinatura, "2.16.840.1.101.3.4.2.3");
        // new AssinadorPdfToken().validarHash(hash, assinatura, "2.16.840.1.101.3.4.2.1");

        new AssinadorPdfToken().validarPorHash(hash, assinatura, SignerAlgorithmEnum.SHA512withRSA);

        assinadorPdf.sign(assinatura);// backend assina o PDF com assinatura recebida do frontend

        assinadorPdf.close();
    }

    private static File gerarImagem() throws IOException {
        return EstampaUtil.gerarEstampa(
            "Organização",
            "Fulano Cicrano Beltrano Fulano Cicrano",
            "012.345.678-90",
            "COORDENADOR DE LICITACOES DE SERVIÇOS ADM. E AQUISICOES DE BENS E CONTRATOS",
            "08/01/2018 17:15",
            "http://assinador.org.br/validacao");
    }

    private static File recuperarImagem() {
        return new File("~/assinador/estampa4450810727878248742.png");
    }

}
