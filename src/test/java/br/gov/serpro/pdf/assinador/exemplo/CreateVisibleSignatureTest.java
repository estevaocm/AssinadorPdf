/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.gov.serpro.pdf.assinador.exemplo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

import br.gov.serpro.pdf.assinador.exemplo.CreateVisibleSignature;

public class CreateVisibleSignatureTest
{
    private static final String inDir = "src/test/resources/exemplo/entrada/";
    private static final String outDir = "src/test/resources/exemplo/saida/";
    private static final String keystorePath = inDir + "keystore.p12";
    private static final String jpegPath = inDir + "05234333442_07-01-2018.png";
    private static final String password = "123456";

    public static boolean externallySign;
    
    public static void main(String[] args) {
    	try {
			testCreateVisibleFirstSignature();
			testCreateVisibleSecondSignature();
		} catch (OperatorCreationException | IOException | CMSException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}    
    
    /**
     * Test creating visual signature.
     *
     * @throws IOException
     * @throws CMSException
     * @throws OperatorCreationException
     * @throws GeneralSecurityException
     */
    public static void testCreateVisibleFirstSignature()
            throws IOException, CMSException, OperatorCreationException, GeneralSecurityException
    {
        // load the keystore
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new FileInputStream(keystorePath), password.toCharArray());

        // sign PDF
        String inPath = inDir + "oficio.pdf";
        File destFile;
        try (FileInputStream fis = new FileInputStream(jpegPath))
        {
            CreateVisibleSignature signing = new CreateVisibleSignature(keystore, password.toCharArray());
            signing.setVisibleSignDesigner(inPath, 100, 700, -40, fis, 1);
            signing.setVisibleSignatureProperties("name", "location", "Security", 0, 1, true);
            signing.setExternalSigning(externallySign);
            destFile = new File(outDir + getOutputFileName("oficio_fabio_visible.pdf"));
            signing.signPDF(new File(inPath), destFile, null);
        }

        checkSignature(destFile);
    }
    
    /**
     * Test creating visual signature.
     *
     * @throws IOException
     * @throws CMSException
     * @throws OperatorCreationException
     * @throws GeneralSecurityException
     */
    public static void testCreateVisibleSecondSignature()
            throws IOException, CMSException, OperatorCreationException, GeneralSecurityException
    {
        // load the keystore
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new FileInputStream(keystorePath), password.toCharArray());

        // sign PDF
        String inPath = inDir + "oficio_fabio_visible.pdf";
        File destFile;
        try (FileInputStream fis = new FileInputStream(jpegPath))
        {
            CreateVisibleSignature signing = new CreateVisibleSignature(keystore, password.toCharArray());
            signing.setVisibleSignDesigner(inPath, 310, 700, -60, fis, 1);
            signing.setVisibleSignatureProperties("name", "location", "Security", 0, 1, true);
            signing.setExternalSigning(externallySign);
            destFile = new File(outDir + getOutputFileName("oficio_fabio_visible2.pdf"));
            signing.signPDF(new File(inPath), destFile, null);
        }

        checkSignature(destFile);
    }
    
    // This check fails with a file created with the code before PDFBOX-3011 was solved.
    private static void checkSignature(File file)
            throws IOException, CMSException, OperatorCreationException, GeneralSecurityException
    {
        try (PDDocument document = PDDocument.load(file))
        {
            List<PDSignature> signatureDictionaries = document.getSignatureDictionaries();
            if (signatureDictionaries.isEmpty())
            {
                //TODO: "no signature found"
            }
            for (PDSignature sig : document.getSignatureDictionaries())
            {
                COSString contents = (COSString) sig.getCOSObject().getDictionaryObject(COSName.CONTENTS);
                byte[] buf;
                try (FileInputStream fis = new FileInputStream(file))
                {
                    buf = sig.getSignedContent(fis);
                }
                CMSSignedData signedData = new CMSSignedData(new CMSProcessableByteArray(buf), contents.getBytes());
                Store certificatesStore = signedData.getCertificates();
                Collection<SignerInformation> signers = signedData.getSignerInfos().getSigners();
                SignerInformation signerInformation = signers.iterator().next();
                Collection matches = certificatesStore.getMatches(signerInformation.getSID());
                X509CertificateHolder certificateHolder = (X509CertificateHolder) matches.iterator().next();
                X509Certificate certFromSignedData = new JcaX509CertificateConverter().getCertificate(certificateHolder);              
                
                // CMSVerifierCertificateNotValidException means that the keystore wasn't valid at signing time
                if (!signerInformation.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certFromSignedData)))
                {
                	//TODO: "Signature verification failed"
                }
                break;
            }
        }
    }
    
    private static String getOutputFileName(String filePattern)
    {
        return MessageFormat.format(filePattern,(externallySign ? "_ext" : ""));
    }
}
