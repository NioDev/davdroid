/*******************************************************************************
 * Copyright (c) 2014 Richard Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Richard Hirner (bitfire web engineering) - initial API and implementation
 ******************************************************************************/
package at.bitfire.davdroid.webdav;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import android.util.Log;
import at.bitfire.davdroid.Constants;
import ch.boye.httpclientandroidlib.client.config.RequestConfig;
import ch.boye.httpclientandroidlib.config.Registry;
import ch.boye.httpclientandroidlib.config.RegistryBuilder;
import ch.boye.httpclientandroidlib.conn.socket.ConnectionSocketFactory;
import ch.boye.httpclientandroidlib.conn.ssl.SSLConnectionSocketFactory;
import ch.boye.httpclientandroidlib.conn.ssl.SSLContextBuilder;
import ch.boye.httpclientandroidlib.conn.ssl.SSLContexts;
import ch.boye.httpclientandroidlib.conn.ssl.TrustStrategy;
import ch.boye.httpclientandroidlib.conn.ssl.X509HostnameVerifier;
import ch.boye.httpclientandroidlib.impl.client.CloseableHttpClient;
import ch.boye.httpclientandroidlib.impl.client.HttpClients;
import ch.boye.httpclientandroidlib.impl.conn.ManagedHttpClientConnectionFactory;
import ch.boye.httpclientandroidlib.impl.conn.PoolingHttpClientConnectionManager;

public class DavHttpClient {

	protected final static RequestConfig defaultRqConfig;

	static {
		// use request defaults from AndroidHttpClient
		defaultRqConfig = RequestConfig.copy(RequestConfig.DEFAULT)
				.setConnectTimeout(20 * 1000).setSocketTimeout(20 * 1000)
				.setStaleConnectionCheckEnabled(false).build();

		// enable logging
		ManagedHttpClientConnectionFactory.INSTANCE.wirelog.enableDebug(true);
		ManagedHttpClientConnectionFactory.INSTANCE.log.enableDebug(true);
	}

	public static CloseableHttpClient create() {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		// limits per DavHttpClient (= per DavSyncAdapter extends
		// AbstractThreadedSyncAdapter)
		connectionManager.setMaxTotal(2); // max. 2 connections in total
		connectionManager.setDefaultMaxPerRoute(2); // max. 2 connections per
													// host

		return HttpClients.custom().useSystemProperties()
				.setSSLSocketFactory(TlsSniSocketFactory.INSTANCE)
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(defaultRqConfig)
				.setUserAgent("DAVdroid/" + Constants.APP_VERSION)
				.disableCookieManagement().build();
	}


	public static CloseableHttpClient createByTrustingAllCert() {
		try {

			SSLContextBuilder builder = SSLContexts.custom();
			// Allow all cert.
			builder.loadTrustMaterial(null, new TrustStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
					return true;
				}
			});
			SSLContext sslContext = builder.build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
					sslContext, new X509HostnameVerifier() {
						@Override
						public void verify(String host, SSLSocket ssl)
								throws IOException {
						}

						@Override
						public void verify(String host, X509Certificate cert)
								throws SSLException {
						}

						@Override
						public void verify(String host, String[] cns,
								String[] subjectAlts) throws SSLException {
						}

						@Override
						public boolean verify(String s, SSLSession sslSession) {
							return true;
						}
					});

			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
					.<ConnectionSocketFactory> create()
					.register("https", sslsf).build();

			PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
					socketFactoryRegistry);

			// limits per DavHttpClient (= per DavSyncAdapter extends
			// AbstractThreadedSyncAdapter)
			connectionManager.setMaxTotal(2); // max. 2 connections in total
			connectionManager.setDefaultMaxPerRoute(2); // max. 2 connections
														// per host

			return HttpClients.custom().useSystemProperties()
					.setConnectionManager(connectionManager)
					.setDefaultRequestConfig(defaultRqConfig)
					.setUserAgent("DAVdroid/" + Constants.APP_VERSION)
					.disableCookieManagement().build();

		} catch (Exception e) {
			Log.e("DavHttpClient", "can not create trust all cert client");
		}
		return create();

	}

}
