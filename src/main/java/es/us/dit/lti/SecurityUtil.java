/*
    This file is part of Tool Provider Manager - Manager of LTI Tool Providers
    for learning platforms.
    Copyright (C) 2022  Francisco José Fernández Jiménez.

    Tool Provider Manager is free software: you can redistribute it and/or
    modify it under the terms of the GNU General Public License as published
    by the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Tool Provider Manager is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
    Public License for more details.

    You should have received a copy of the GNU General Public License along
    with Tool Provider Manager. If not, see <https://www.gnu.org/licenses/>.
*/

package es.us.dit.lti;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.us.dit.lti.entity.IUpdateRecordEntity;

/**
 * Utility class with security methods (cryptography, hash, random,
 * paswords,...).
 *
 * <p>Uses <code>PBKDF2WithHmacSHA512</code> for password hash generation,
 * <code>SHA1PRNG</code> for random number generation, and <code>AES GCM</code>
 * for encryption.
 *
 * @author Francisco José Fernández Jiménez
 *
 */
public final class SecurityUtil {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SecurityUtil.class);

	// Random
	/**
	 * Algorithm for random number generator.
	 */
	private static final String RANDOM_ALGORITHM = "SHA1PRNG";
	/**
	 * Secure random number generator.
	 */
	private static final SecureRandom random;

	// Paswords
	// See
	// https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
	/**
	 * Algorithm for password hashing.
	 */
	private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA512";
	/**
	 * Iterations of KEY_ALGORITHM by default.
	 */
	private static final int DEFAULT_ITERATIONS = 120000;
	/**
	 * Key length of KEY_ALGORITHM.
	 */
	private static final int KEY_LENGTH = 512;
	/**
	 * Key factory for password hashing.
	 */
	private static final SecretKeyFactory keyFactory;

	// Encrypted tokens
	// Random key for cipher sids
	/**
	 * Random key for encrypt serial IDs.
	 */
	private static final SecretKey eKey;
	/**
	 * GCM IV length.
	 */
	private static final int GCM_IV_LENGTH = 12;
	/**
	 * GCM tag length (bytes).
	 */
	private static final int GCM_TAG_LENGTH = 16;
	/**
	 * GCM associated data for verification.
	 */
	private static final byte[] associatedData;
	/**
	 * timeout in case of encryption error to avoid brute force attacks.
	 */
	private static final long ERROR_INTERVAL = 6000L; // 6 seconds

	// Initialize all class members.
	static {
		SecureRandom aux = null;
		try {
			aux = SecureRandom.getInstance(RANDOM_ALGORITHM);
		} catch (final NoSuchAlgorithmException e) {
			// ignore
			logger.error("RANDOM ALGORITHM", e);
		}
		if (aux != null) {
			random = aux;
		} else {
			random = new SecureRandom();
		}

		try {
			keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
		} catch (final NoSuchAlgorithmException e) {
			throw new ExceptionInInitializerError(e);
		}

		Security.setProperty("crypto.policy", "unlimited");
		KeyGenerator keyGeneratorE = null;
		try {
			keyGeneratorE = KeyGenerator.getInstance("AES");
		} catch (final NoSuchAlgorithmException e) {
			keyGeneratorE = null;
		}
		if (keyGeneratorE != null) {
			keyGeneratorE.init(256);
			eKey = keyGeneratorE.generateKey();
		} else {
			eKey = null;
		}
		associatedData = SecurityUtil.class.getPackageName().getBytes(StandardCharsets.UTF_8); // to verify
	}

	/**
	 * Container class for results of
	 * {@link SecurityUtil#getPlainSecuredSid(String, long)}.
	 *
	 * <p>All members are public to ease access.
	 *
	 * @author Francisco José Fernández Jiménez
	 *
	 */
	public static class SecuredSid {
		/**
		 * Plain sid.
		 */
		public int sid = 0;
		/**
		 * Verifier.
		 *
		 * <p>Used when sid can be reused.
		 */
		public long verifier = 0;
	}

	/**
	 * Can not create objects.
	 */
	private SecurityUtil() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Gets a random id for CSRF protection.
	 *
	 * @return a base64 encoded random id
	 */
	public static String getRandomId() {
		final byte[] id = new byte[KEY_LENGTH / 8];
		random.nextBytes(id);
		return Base64.encodeBase64String(id);
	}

	/**
	 * Gets a password hast to storage in db, using default number of iterations and
	 * a random salt.
	 *
	 * <p>Using <a href=
	 * "https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">OWASP
	 * guidelines</a>.
	 *
	 * @param password password in plain text
	 * @return the password hash
	 * @throws InvalidKeySpecException if security algorithms are not supported
	 */
	public static String getPasswordHash(String password) throws InvalidKeySpecException {
		return getPasswordHash(password, DEFAULT_ITERATIONS);
	}

	/**
	 * Gets a password hast to storage in db, using specific number of iterations
	 * and a random salt.
	 *
	 * <p>Using <a href=
	 * "https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">OWASP
	 * guidelines</a>.
	 *
	 * @param password   password in plain text
	 * @param iterations the number of iterations of the algorithm
	 * @return the password hash
	 * @throws InvalidKeySpecException if security algorithms are not supported
	 */
	public static String getPasswordHash(String password, int iterations) throws InvalidKeySpecException {
		final byte[] salt = new byte[64];
		random.nextBytes(salt);
		return getPasswordHash(password, iterations, salt);
	}

	/**
	 * Gets a password hast to storage in db, using specific number of iterations
	 * and a specific salt.
	 *
	 * <p>Using <a href=
	 * "https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">OWASP
	 * guidelines</a>.
	 *
	 * @param password   password in plain text
	 * @param iterations the number of iterations of the algorithm
	 * @param salt       the salt
	 * @return the password hash
	 * @throws InvalidKeySpecException if security algorithms are not supported
	 */
	public static String getPasswordHash(String password, int iterations, byte[] salt) throws InvalidKeySpecException {
		final PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
		return "$" + KEY_ALGORITHM + "$" + iterations + "$" + Base64.encodeBase64String(salt) + "$"
				+ Base64.encodeBase64String(keyFactory.generateSecret(keySpec).getEncoded());
	}

	/**
	 * Check a password hash against a plain password.
	 *
	 * @param passwordHash the password hash
	 * @param password     the plain password
	 * @return true if valid
	 */
	public static boolean checkPassword(String passwordHash, String password) {
		boolean result = false;
		// Extract algorithm, iterations, salt
		final String[] sections = passwordHash.split("\\$");
		if (sections.length == 5 && sections[1].equals(KEY_ALGORITHM)) {
			try {
				final byte[] hash = Base64.decodeBase64(sections[4]);
				final PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), Base64.decodeBase64(sections[3]),
						Integer.parseInt(sections[2]), hash.length * 8);
				final byte[] newHash = keyFactory.generateSecret(keySpec).getEncoded();
				result = Arrays.equals(hash, newHash);

			} catch (NumberFormatException | InvalidKeySpecException e) {
				// Ignore
				logger.error("Exception: {}", e.getMessage());
			}
		}

		return result;
	}

	/**
	 * Generate a IV with a valid length for encryption.
	 *
	 * @return the generated IV
	 */
	private static byte[] generateIv() {
		final byte[] iv = new byte[GCM_IV_LENGTH];
		random.nextBytes(iv);
		return iv;
	}

	/**
	 * Encrypt plain text with <code>"AES/GCM/NoPadding"</code> algorithm.
	 *
	 * @param plaintext the plain text
	 * @param iv        IV used in algorithm
	 * @return the cipher text
	 * @throws NoSuchAlgorithmException           if cipher suite not supported
	 * @throws NoSuchPaddingException             if cipher suite not supported
	 * @throws InvalidKeyException                if cipher suite not supported
	 * @throws InvalidAlgorithmParameterException if cipher suite not supported
	 * @throws IllegalBlockSizeException          if cipher suite not supported
	 * @throws BadPaddingException                if cipher suite not supported
	 */
	private static byte[] encrypt(byte[] plaintext, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		final SecretKeySpec keySpec = new SecretKeySpec(eKey.getEncoded(), "AES");
		final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
		cipher.updateAAD(associatedData);
		return cipher.doFinal(plaintext);
	}

	/**
	 * Decrypt cipher text with <code>"AES/GCM/NoPadding"</code> algorithm.
	 *
	 * @param cipherText the cipher text
	 * @param iv         IV used in algorithm
	 * @return the plain text
	 * @throws NoSuchAlgorithmException           if cipher suite not supported
	 * @throws NoSuchPaddingException             if cipher suite not supported
	 * @throws InvalidKeyException                if cipher suite not supported
	 * @throws InvalidAlgorithmParameterException if cipher suite not supported
	 * @throws IllegalBlockSizeException          if cipher suite not supported
	 * @throws BadPaddingException                if cipher suite not supported
	 */
	private static byte[] decrypt(byte[] cipherText, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		final SecretKeySpec keySpec = new SecretKeySpec(eKey.getEncoded(), "AES");
		final GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
		cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
		cipher.updateAAD(associatedData);
		return cipher.doFinal(cipherText);
	}

	/**
	 * Gets a secure serial ID from the serial ID of a db entity. A secured sid
	 * includes de sid, the creation date (verifier) and the serial version UID of
	 * the entity. It is encrypted with a random key.
	 *
	 * @param entity the entity whose serial id will be encrypted
	 * @return the secured sid
	 */
	public static String getSecureSid(IUpdateRecordEntity entity) {
		final byte[] bytes = ByteBuffer.allocate(20).putInt(entity.getSid()).putLong(entity.getSerialVersionUid())
				.putLong(entity.getCreated().getTimeInMillis()).array();
		// encriptar data con una clave aleatoria de la aplicación, MAC
		final byte[] iv = generateIv();
		String token;
		try {
			final byte[] ciphertext = encrypt(bytes, iv);
			final byte[] container = new byte[ciphertext.length + iv.length];
			System.arraycopy(iv, 0, container, 0, iv.length);
			System.arraycopy(ciphertext, 0, container, iv.length, ciphertext.length);
			token = Base64.encodeBase64URLSafeString(container);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			logger.warn("Generating without encryption");
			token = Base64.encodeBase64URLSafeString(bytes);
		}
		return token;
	}

	/**
	 * Extracts the plain sid and verifier (creation date) of a secured sid.
	 *
	 * @param data             the secured sid
	 * @param serialVersionUid the serial version UID of the entity whose sid is
	 *                         secured
	 * @return the plain sid and the verifier.
	 */
	public static SecuredSid getPlainSecuredSid(String data, long serialVersionUid) {
		final SecuredSid sid = new SecuredSid();
		try {
			final byte[] container = Base64.decodeBase64(data);
			final byte[] iv = generateIv();
			if (container.length - iv.length > 0) {
				final byte[] ciphertext = new byte[container.length - iv.length];
				System.arraycopy(container, 0, iv, 0, iv.length);
				System.arraycopy(container, iv.length, ciphertext, 0, ciphertext.length);
				final ByteBuffer bb = ByteBuffer.wrap(decrypt(ciphertext, iv));
				sid.sid = bb.getInt();
				final long svuid = bb.getLong();
				if (svuid == serialVersionUid) {
					sid.verifier = bb.getLong();
				} else {
					sid.sid = 0;
				}
			} else {
				throw new IllegalBlockSizeException("");
			}
		} catch (BufferUnderflowException | IllegalArgumentException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
				| BadPaddingException e1) {
			// add measures to prevent brute force attacks
			logger.error("Invalid SecureSid");
			try {
				Thread.sleep(ERROR_INTERVAL);
			} catch (final InterruptedException e) {
				// Restore interrupted state...
				Thread.currentThread().interrupt();
			}
		}
		return sid;
	}

}
