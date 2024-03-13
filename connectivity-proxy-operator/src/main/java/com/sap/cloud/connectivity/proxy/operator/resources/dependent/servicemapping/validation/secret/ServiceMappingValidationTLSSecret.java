package com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.validation.secret;

import static com.sap.cloud.connectivity.operator.core.constants.ConnectivityProxyConstants.CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_FULL_NAME;
import static org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider.PROVIDER_NAME;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import com.sap.cloud.connectivity.operator.core.ConnectivityProxy;
import com.sap.cloud.connectivity.proxy.operator.resources.dependent.servicemapping.ServiceMappingsDependentResource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public final class ServiceMappingValidationTLSSecret extends ServiceMappingsDependentResource<Secret> {

	private static final Logger LOGGER = LogManager.getLogger(ServiceMappingValidationTLSSecret.class);
	private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
	private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

	private static final long NOT_BEFORE_TOLERANCE_FIVE_SECONDS_IN_MILLISECONDS = 1000L * 5;
	private static final long NOT_AFTER_ONE_YEAR_IN_MILLISECONDS = 1000L * 60 * 60 * 24 * 365;
	private static final long EXPIRATION_TOLERANCE_THIRTY_DAYS_IN_MILLISECONDS = 1000L * 60 * 60 * 24 * 30;

	private static final String PRIVATE_KEY_ALGORITHM = "RSA";
	private static final int KEY_SIZE = 4096;
	private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

	private static final String CERTIFICATE_PEM_TYPE = "CERTIFICATE";
	private static final String RSA_PRIVATE_KEY_PEM_TYPE = "RSA PRIVATE KEY";

	private static final String CA_CERT_SECRET_DATA_KEY = "ca.crt";
	private static final String TLS_CERT_SECRET_DATA_KEY = "tls.crt";
	private static final String TLS_KEY_SECRET_KEY = "tls.key";

	public ServiceMappingValidationTLSSecret(KubernetesClient kubernetesClient) {
		super(Secret.class, kubernetesClient);
		BouncyCastleFipsProvider bouncyCastleFipsProvider = new BouncyCastleFipsProvider();
		Security.addProvider(bouncyCastleFipsProvider);
	}

	/**
	 * The SM Operator Deployment needs to be restarted, in order to pick up the updated certificate contained in the Secret
	 */
	@Override
	public Secret update(Secret actual, Secret target, ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		Secret updated = super.update(actual, target, connectivityProxy, context);

		RollableScalableResource<Deployment> rollableScalableResource = getKubernetesClient().apps()
																							 .deployments()
																							 .inNamespace(connectivityProxy.installationNamespace())
																							 .withName(connectivityProxy.serviceMappingsOperatorFullName());
		Deployment deployment = rollableScalableResource.get();
		if (deployment != null) {
			LOGGER.info("Restarting SM Operator Deployment because SM TLS Secret was updated!");
			rollableScalableResource.rolling().restart();
		}
		return updated;
	}

	@Override
	protected ResourceIDMatcherDiscriminator<Secret, ConnectivityProxy> createResourceDiscriminator() {
		return new ResourceIDMatcherDiscriminator<>(
				connectivityProxy -> new ResourceID(getDependentResourceName(connectivityProxy), getDependentResourceNamespace(connectivityProxy)));
	}

	@Override
	protected String getDependentResourceName(ConnectivityProxy connectivityProxy) {
		return CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_FULL_NAME;
	}

	@Override
	protected String getDependentResourceNamespace(ConnectivityProxy connectivityProxy) {
		return connectivityProxy.installationNamespace();
	}

	@Override
	protected Secret desired(ConnectivityProxy connectivityProxy, Context<ConnectivityProxy> context) {
		return new SecretBuilder()
				.withType("kubernetes.io/tls")
				.withMetadata(createMetadata(connectivityProxy))
				.withData(createData(connectivityProxy))
				.build();
	}

	private ObjectMeta createMetadata(ConnectivityProxy connectivityProxy) {
		return new ObjectMetaBuilder()
				.withName(getDependentResourceName(connectivityProxy))
				.withNamespace(getDependentResourceNamespace(connectivityProxy))
				.withLabels(getManagedByLabel())
				.build();
	}

	/*
	 * Generating a self-signed certificate on every reconciliation is expensive and requires restart of the SM Operator
	 * Deployment. So we get current secret data from cluster and if the Secret doesn't exist, certificate is nearing
	 * expiration or some of its fields have been removed, we generate a new one.
	 */
	private Map<String, String> createData(ConnectivityProxy connectivityProxy) {
		Secret secret = getKubernetesClient().secrets()
											 .inNamespace(connectivityProxy.installationNamespace())
											 .withName(CONNECTIVITY_SERVICE_MAPPINGS_VALIDATION_FULL_NAME)
											 .get();

		if (secret == null) {
			return generateSelfSignedCertificate(connectivityProxy.createSMValidationServiceFQN());
		}

		String caCert = secret.getData().get(CA_CERT_SECRET_DATA_KEY);
		String tlsCert = secret.getData().get(TLS_CERT_SECRET_DATA_KEY);
		String tlsKey = secret.getData().get(TLS_KEY_SECRET_KEY);

		X509CertificateHolder x509Certificate = parseCertificateFromPEMFormat(new String(BASE64_DECODER.decode(caCert)));

		if (caCert != null && tlsCert != null && tlsKey != null && checkCertificateExpirationIsWithinTolerance(x509Certificate)) {
			return Map.of(CA_CERT_SECRET_DATA_KEY, caCert,
						  TLS_CERT_SECRET_DATA_KEY, tlsCert,
						  TLS_KEY_SECRET_KEY, tlsKey
			);
		} else {
			LOGGER.info("ServiceMapping Validation TLS Secret is present but certificate is expiring soon, so regenerating it now!");
			return generateSelfSignedCertificate(connectivityProxy.createSMValidationServiceFQN());
		}
	}

	private X509CertificateHolder parseCertificateFromPEMFormat(String caCertDecoded) {
		try (PEMParser pemParser = new PEMParser(new StringReader(caCertDecoded))) {
			return (X509CertificateHolder) pemParser.readObject();
		} catch (IOException e) {
			throw new IllegalStateException("Error parsing ServiceMapping Validation TLS certificate", e);
		}
	}

	private boolean checkCertificateExpirationIsWithinTolerance(X509CertificateHolder x509CertificateHolder) {
		return System.currentTimeMillis() + EXPIRATION_TOLERANCE_THIRTY_DAYS_IN_MILLISECONDS < x509CertificateHolder.getNotAfter().getTime();
	}

	/**
	 * Generates a self-signed x509 server certificate, using FIPS-compliant BouncyCastle APIs.
	 * 
	 * @param host
	 *            the host which is set as CN and as a DNS name in the SubjectAlternativeNames
	 * @return Map containing the certificate and the private key
	 */
	private Map<String, String> generateSelfSignedCertificate(String host) {
		try {
			LOGGER.info("Generating a self-signed certificate for host {}", host);

			KeyPair keyPair = generateKeyPair(PRIVATE_KEY_ALGORITHM, KEY_SIZE);
			LOGGER.info("Generated certificate key pair successfully!");

			X500Principal subject = new X500Principal("CN=" + host);
			long notBefore = System.currentTimeMillis() - NOT_BEFORE_TOLERANCE_FIVE_SECONDS_IN_MILLISECONDS;
			long notAfter = notBefore + NOT_AFTER_ONE_YEAR_IN_MILLISECONDS;

			X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(subject, BigInteger.valueOf(notBefore), new Date(notBefore),
					new Date(notAfter), subject, keyPair.getPublic());
			addCertificateExtensions(host, certBuilder);

			final ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(PROVIDER_NAME).build(keyPair.getPrivate());
			X509CertificateHolder certHolder = certBuilder.build(signer);
			LOGGER.info("Signed certificate successfully!");

			String certBase64 = convertCertificateToBase64PEMFormat(certHolder);
			String privateKeyBase64 = convertPrivateKeyToBase64PEMFormat(keyPair.getPrivate());

			return Map.of(CA_CERT_SECRET_DATA_KEY, certBase64,
						  TLS_CERT_SECRET_DATA_KEY, certBase64,
						  TLS_KEY_SECRET_KEY, privateKeyBase64);
		} catch (Exception e) {
			throw new IllegalStateException("Error generating ServiceMapping Validation TLS certificate", e);
		}
	}

	private KeyPair generateKeyPair(String algorithm, Integer keySize) throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
		keyPairGenerator.initialize(keySize, new SecureRandom());
		return keyPairGenerator.generateKeyPair();
	}

	private static void addCertificateExtensions(String host, X509v3CertificateBuilder certBuilder) throws CertIOException {
		KeyUsage keyUsage = new KeyUsage(KeyUsage.digitalSignature + KeyUsage.keyEncipherment);
		KeyPurposeId[] extendedKeyUsage = new KeyPurposeId[] { KeyPurposeId.id_kp_serverAuth };
		ASN1Encodable[] encodableAltNames = new ASN1Encodable[] { new GeneralName(GeneralName.dNSName, host) };

		certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
		certBuilder.addExtension(Extension.keyUsage, true, keyUsage);
		certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(extendedKeyUsage));
		certBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(encodableAltNames));
	}

	private String convertCertificateToBase64PEMFormat(X509CertificateHolder certHolder) throws IOException {
		byte[] encodedCert = certHolder.toASN1Structure().getEncoded();
		String certificateToPEM = encodeObjectToPEM(new PemObject(CERTIFICATE_PEM_TYPE, encodedCert));

		return BASE64_ENCODER.encodeToString(certificateToPEM.getBytes());
	}

	private String convertPrivateKeyToBase64PEMFormat(PrivateKey privateKey) throws IOException {
		PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
		byte[] privateKeyEncoded = privateKeyInfo.parsePrivateKey().toASN1Primitive().getEncoded();
		String privateKeyToPEM = encodeObjectToPEM(new PemObject(RSA_PRIVATE_KEY_PEM_TYPE, privateKeyEncoded));

		return BASE64_ENCODER.encodeToString(privateKeyToPEM.getBytes());
	}

	private String encodeObjectToPEM(PemObject pemObject) throws IOException {
		try (StringWriter stringWriter = new StringWriter()) {
			try (PemWriter pemWriter = new JcaPEMWriter(stringWriter)) {
				pemWriter.writeObject(pemObject);
			}
			return stringWriter.toString();
		}
	}

}
