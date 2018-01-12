package hentrope.runeframe.util;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.zip.ZipInputStream;

import javax.naming.NamingException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Handles the verification of certificates used to sign code inside of
 * the gamepack.
 * 
 * @author hentrope
 * @see CertificateVerifier#verify(Certificate[])
 */
public class CertificateVerifier {
	private final File directory;
	private final boolean includeNative;

	private Set<X509Certificate> certificates;
	private Date date;
	private TrustManagerFactory tmf;
	private CertificateFactory cf;
	private Certificate[] cache;

	public CertificateVerifier(File directory, boolean includeNative) {
		this.directory = directory;
		this.includeNative = includeNative;
	}

	public CertificateVerifier(File directory) {
		this.directory = directory;
		this.includeNative = false;
	}

	/**
	 * Verifies whether the given chain of certificates can be trusted based on
	 * the certificates currently loaded by the verifier.
	 * 
	 * @param certs the chain of certificates to be verified, where the last
	 * index contains the certificate of a root CA
	 * @throws GeneralSecurityException if the chain of certificates cannot be trusted
	 */
	public void verify(Certificate[] certs) throws GeneralSecurityException {
		// Use lazy initialization to load the list of certificates
		if (certificates == null)
			load();

		// Reject null certificate chain.
		if (certs == null)
			throw new CertificateException("Null certificate chain.");

		// Check the provided certificate array against the cached array. If the two are equal,
		// immediately return without further checks.
		if (cache != null && certs.length == cache.length) {
			boolean equal = true;
			for (int i = 0; i < certs.length; i++)
				equal = equal && certs[i] == cache[i];
			if (equal)
				return;
		}

		// For the RuneScape client, the certificate chain length should always be greater than one.
		if (certs.length < 2)
			throw new CertificateException("Length of certificate chain is less than 2.");

		// Verify that all certificate in the chain extend X509Certificate
		for (int i = 0; i < certs.length; i++)
			if (certs[i] instanceof X509Certificate == false)
				throw new CertificateException("Certificate is not an instance of X509Certificate.");

		// Verify that the last certificate in the chain is a trusted certificate authority.
		X509Certificate last = (X509Certificate) certs[certs.length - 1];
		if (!certificates.contains(last))
			throw new CertificateException("Last certificate in chain is not a trusted certificate authority.");

		// Since all CA certificates are checked for validity, this is unnecessary.
		// last.checkValidity();

		String rootOrg = getOrganization(last);
		for (int i = certs.length - 2; i >= 0; i--) {
			X509Certificate cert = (X509Certificate) certs[i];

			// Make sure that all intermediate certificates are for the same organization as the root.
			if ( i > 0 ) {
				if (!rootOrg.equalsIgnoreCase(getOrganization(cert)))
					throw new CertificateException("Intermediate certificates' organizations do not match root.");
			}

			// Make sure the first certificate was issued to Jagex.
			else {
				if (!"Jagex Ltd".equalsIgnoreCase(getOrganization(cert)))
					throw new CertificateException("First certificate organization must be \"Jagex Ltd\".");
			}

			cert.verify(certs[i+1].getPublicKey());
			cert.checkValidity();
		}

		cache = certs;
	}

	private String getOrganization(X509Certificate cert) throws CertificateException {
		try {
			String dn = cert.getSubjectX500Principal().getName();
			LdapName ldapDN = new LdapName(dn);

			for (int i = 0; i < ldapDN.size(); i++) {
				Rdn rdn = ldapDN.getRdn(i);
				if ("O".equals(rdn.getType()))
					return rdn.getValue().toString();
			}
		} catch (NamingException e) {}

		throw new CertificateException("Organization name not found.");
	}

	private void load() {
		certificates = new HashSet<X509Certificate>();
		date = new Date();

		loadInternal();

		File[] files = directory.listFiles();
		if (files != null)
			loadDirectory(files);

		if (includeNative)
			loadNative();
	}

	@SuppressWarnings("unchecked")
	private void loadInternal() {
		try (	InputStream file = CertificateVerifier.class.getClassLoader().getResourceAsStream("resource/certificates.zip");
				ZipInputStream zip = new ZipInputStream(file) ) {
			if (cf == null)
				cf = CertificateFactory.getInstance("X.509");

			while (zip.getNextEntry() != null) {
				try {
					for (X509Certificate cert : (Collection<X509Certificate>) cf.generateCertificates(zip)) {
						try {
							cert.checkValidity(date);
							certificates.add(cert);
						} catch (CertificateException e) {}
					} 
				} catch (ClassCastException e) {
					System.err.println("Encountered a collection other than X509Certificates");
				}
			}
		} catch (CertificateException | IOException | NullPointerException e) {}
	}

	private void loadDirectory(File[] files) {
		try {
			if (cf == null)
				cf = CertificateFactory.getInstance("X.509");

			for (File file : files)
				if (file.getName().endsWith(".pem"))
					loadFile(file);
		} catch (CertificateException e) {}
	}

	@SuppressWarnings("unchecked")
	private void loadFile(File file) {
		try {
			for (X509Certificate cert : (Collection<X509Certificate>) cf.generateCertificates(new FileInputStream(file))) {
				try {
					cert.checkValidity(date);
					certificates.add(cert);
				} catch (CertificateException e) {}
			}
		} catch (ClassCastException e) {
			System.err.println("Encountered a collection other than X509Certificates");
		} catch (CertificateException | FileNotFoundException e) {}
	}

	private void loadNative() {
		try {
			if (tmf == null) {
				tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init((KeyStore)null);
			}

			for (TrustManager tm: tmf.getTrustManagers()) {
				if (tm instanceof X509TrustManager) {
					for (X509Certificate cert: ((X509TrustManager)tm).getAcceptedIssuers()) {
						try {
							cert.checkValidity(date);
							certificates.add(cert);
						} catch (CertificateException e) {}
					}
				} else
					System.err.println("Encountered a TrustManager which was not a decendant of X509TrustManager");
			}
		} catch (GeneralSecurityException e) {}
	}
}
