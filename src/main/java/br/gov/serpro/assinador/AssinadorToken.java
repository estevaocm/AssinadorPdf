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
package br.gov.serpro.assinador;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.commons.io.IOUtils;
import org.demoiselle.signer.core.ca.manager.CAManager;
import org.demoiselle.signer.core.keystore.loader.KeyStoreLoader;
import org.demoiselle.signer.core.keystore.loader.configuration.Configuration;
import org.demoiselle.signer.core.keystore.loader.factory.KeyStoreLoaderFactory;
import org.demoiselle.signer.policy.engine.factory.PolicyFactory;
import org.demoiselle.signer.policy.engine.factory.PolicyFactory.Policies;
import org.demoiselle.signer.policy.impl.cades.SignatureInformations;
import org.demoiselle.signer.policy.impl.cades.SignerAlgorithmEnum;
import org.demoiselle.signer.policy.impl.cades.factory.PKCS7Factory;
import org.demoiselle.signer.policy.impl.cades.pkcs7.PKCS7Signer;
import org.demoiselle.signer.policy.impl.cades.pkcs7.impl.CAdESChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assina um vetor de bytes com uma chave privada proveniente de token.
 * @author Estêvão Monteiro, Renê Campanário
 * @since 23/10/17
 */
public class AssinadorToken{

	private static final Logger L = LoggerFactory.getLogger(AssinadorToken.class); 

	protected CallbackHandler callbackHandler;
	protected PKCS7Signer signer;
	protected X509Certificate certificate;
	protected Certificate[] chain;

	public AssinadorToken() throws IOException, GeneralSecurityException{
		L.info("Iniciando " + getClass().getSimpleName());
		L.info(System.getProperty("java.vendor") + " Java " + System.getProperty("java.version"));
		//IMPORTANTE: Nunca permitir que métodos chamados no construtor sejam sobrescritos em subclasses.
		this.signer = newSigner();
		this.callbackHandler = getCallbackHandler();
	}

	/**
	 * Tratador do retorno do diálogo para informar o PIN.
	 * @return
	 * @throws IOException
	 */
	private CallbackHandler getCallbackHandler() throws IOException{
		return new CallbackHandler() {
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				/*
				Não é necessário fazer nada, o driver fornecerá janela de diálogo para pedir PIN se necessário.
				for (Callback callback : callbacks) {
					if (callback instanceof PasswordCallback) {
						L.info("Solicitando PIN");
						PasswordCallback pwd = ((PasswordCallback)callback);
						L.info("PIN OK");
					}
				}
				 */
			}
		};
	}

	/**
	 * Constroi o assinador de token.
	 * @return
	 * @throws GeneralSecurityException
	 */
	private PKCS7Signer newSigner() throws GeneralSecurityException{

		PKCS7Signer signer = PKCS7Factory.getInstance().factory();

		//TODO parametrizar a política?
		Policies signaturePolicy = PolicyFactory.Policies.AD_RB_CADES_2_2;
		L.info("Configurando política de Assinatura: " + signaturePolicy.getUrl());

		signer.setSignaturePolicy(signaturePolicy);

		return signer;
	}

	private SignerAlgorithmEnum getAlgoritmo() {
		SignerAlgorithmEnum alg = SignerAlgorithmEnum.SHA512withRSA;
		L.info("O algoritmo default é SHA512withRSA");
		if (System.getProperty("java.version").contains("1.8") 
				&& (this.signer.getProvider() != null &&
				this.signer.getProvider().getName().contains("TokenOuSmartCard_30"))) {
			L.info("Detectado token WatchData e Java 8; configurando o algoritmo para Sha256withRSA.");
			alg = SignerAlgorithmEnum.SHA256withRSA;
		}
		if (Configuration.getInstance().getSO().toLowerCase().indexOf("indows") > 0) {
			L.info("Detectado Windows; configurando o algoritmo para Sha256withRSA.");
			alg = SignerAlgorithmEnum.SHA256withRSA;
		}		
		return alg;
	}

	public byte[] sign(byte[] content) {
		if(content == null || content.length == 0){
			throw new IllegalArgumentException("Conteúdo nulo");
		}
		L.info("Bytes para assinar: " + content.length + " bytes");

		try {
			KeyStore keyStore = getTokenKeyStore();
			configSigner(keyStore);

			L.info("Tudo pronto. Assinando ... ");
			//Gera a assinatura avulsa, com o BouncyCastle:
			byte[] assinatura = this.signer.doDetachedSign(content);

			L.info("Assinatura pronta. Tamanho: " + assinatura.length + " bytes");

			validar(content, assinatura);

			return assinatura;
		} 
		catch (Throwable t) {
			L.info("Algo falhou: " + t.getMessage() + (t.getCause()!=null?
					" Causa: "+t.getCause().getMessage():". sem causa."));
			t.printStackTrace();
			return null;
		}
	}

	public byte[] signHash(byte[] hash) {
		if(hash == null || hash.length == 0){
			throw new IllegalArgumentException("Hash nulo");
		}
		L.info("Bytes para assinar: " + hash.length + " bytes");
		try {
			KeyStore keyStore = getTokenKeyStore();
			configSigner(keyStore);

			L.info("Tudo pronto. Assinando ... ");
			//Gera a assinatura avulsa, com o BouncyCastle:
			byte[] assinatura = this.signer.doHashSign(hash);

			L.info("Assinatura pronta. Tamanho: " + assinatura.length + " bytes");//2914 bytes

			return assinatura;
		} 
		catch (Throwable t) {
			L.info("Algo falhou: " + t.getMessage() + (t.getCause()!=null?
					" Causa: "+t.getCause().getMessage():". sem causa."));
			t.printStackTrace();
			return null;
		}
	}

	/**
	 * Solicita o PIN do usuário e recupera a KeyStore do token.
	 * @return
	 * @throws IOException
	 */
	protected KeyStore getTokenKeyStore() throws IOException{

		L.info("Fabricando KeyStoreLoader");

		KeyStoreLoader loader = KeyStoreLoaderFactory.factoryKeyStoreLoader();

		L.info("Definindo callback do PIN");

		loader.setCallbackHandler(this.callbackHandler);

		L.info("Carregando KeyStore");

		KeyStore keyStore = loader.getKeyStore();//Arqui a JVM solicta entrada do PIN do usuário

		String providerName = keyStore.getProvider().toString();
		String tokenConfigName = providerName.split(" ")[0].split("-")[1];
		String pathDriver = Configuration.getInstance().getDrivers().get(tokenConfigName);

		L.info("Provider " + providerName + " @ " + pathDriver);

		return keyStore;
	}

	/**
	 * Configura o PKCS7Signer do Demoiselle para usar o KeyStore recebido.
	 * @param keyStore
	 * @return
	 * @throws GeneralSecurityException
	 */
	protected void configSigner(KeyStore keyStore) throws GeneralSecurityException{

		String alias = keyStore.aliases().nextElement();

		L.info("Alias: " + alias);

		L.info("Pegando o certificado do alias " + alias);
		this.certificate = (X509Certificate)keyStore.getCertificate(alias);

		L.info("Pegando a referencia é chave privada ");
		PrivateKey privateKey = (PrivateKey)keyStore.getKey(alias, null);
		this.signer.setPrivateKey(privateKey);

		L.info("Buscando a cadeia de autoridades do certificado ");
		this.chain = CAManager.getInstance().getCertificateChainArray(certificate);
		this.signer.setCertificates(this.chain);

		this.signer.setProvider(keyStore.getProvider());
		this.signer.setAlgorithm(getAlgoritmo());

	}

	/**
	 * Valida a assinatura, comparando com o conteúdo original.
	 * @param content
	 * @param assinatura
	 */
	public void validar(byte[] content, byte[] assinatura){
		L.info("Validando a assinatura");
		List<SignatureInformations> sigData = new CAdESChecker().checkDetattachedSignature(content, assinatura);

		L.info("A assinatura está válida.");
		for(SignatureInformations si : sigData){
			L.debug(si.getSignDateGMT());
			L.debug(si.getSignersBasicCertificates().toString());
			L.debug(si.getChain().toString());
		}
	}		

	public void validarPorHash(byte[] hash, byte[] assinatura, SignerAlgorithmEnum OIDAlgoritmo){
		L.info("Validando a assinatura");
		List<SignatureInformations> sigData = new CAdESChecker().checkSignatureByHash(
				OIDAlgoritmo.getOIDAlgorithmHash(), hash, assinatura);
		
		L.info("A assinatura está válida.");
		for(SignatureInformations si : sigData){
			L.debug(si.getSignDateGMT());
			L.debug(si.getSignersBasicCertificates().toString());
			L.debug(si.getChain().toString());
		}
	}		

	public Certificate[] getChain() {
		return chain;
	}

	public X509Certificate getCertificate() {
		return certificate;
	}

}