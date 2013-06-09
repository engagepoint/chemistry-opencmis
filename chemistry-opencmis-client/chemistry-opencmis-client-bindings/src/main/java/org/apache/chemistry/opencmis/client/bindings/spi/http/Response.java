/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.chemistry.opencmis.client.bindings.spi.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.apache.chemistry.opencmis.commons.impl.Base64;

/**
 * HTTP Response.
 */
public class Response {
    private final int responseCode;
    private final String responseMessage;
    private final Map<String, List<String>> headers;
    private InputStream stream;
    private String errorContent;
    private BigInteger length;
    private String charset;
    private boolean hasResponseStream;

    public Response(int responseCode, String responseMessage, Map<String, List<String>> headers,
            InputStream responseStream, InputStream errorStream) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.stream = responseStream;
        this.hasResponseStream = (stream != null);

        this.headers = new HashMap<String, List<String>>();
        if (headers != null) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                this.headers.put(e.getKey() == null ? null : e.getKey().toLowerCase(), e.getValue());
            }
        }

        // determine charset
        charset = "UTF-8";
        String contentType = getContentTypeHeader();
        if (contentType != null) {
            String[] parts = contentType.split(";");
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim().toLowerCase();
                if (part.startsWith("charset")) {
                    int x = part.indexOf('=');
                    charset = part.substring(x + 1).trim();
                    break;
                }
            }
        }

        // if there is an error page, get it
        if (errorStream != null) {
            if (contentType != null) {
                String contentTypeLower = contentType.toLowerCase().split(";")[0];
                if (contentTypeLower.startsWith("text/") || contentTypeLower.endsWith("+xml")
                        || contentTypeLower.startsWith("application/xml")
                        || contentTypeLower.startsWith("application/json")) {
                    StringBuilder sb = new StringBuilder();

                    try {
                        String encoding = getContentEncoding();
                        if (encoding != null) {
                            if (encoding.toLowerCase().trim().equals("gzip")) {
                                try {
                                    errorStream = new GZIPInputStream(errorStream, 4096);
                                } catch (IOException e) {
                                }
                            } else if (encoding.toLowerCase().trim().equals("deflate")) {
                                errorStream = new InflaterInputStream(errorStream, new Inflater(true), 4096);
                            }
                        }

                        InputStreamReader reader = new InputStreamReader(errorStream, charset);
                        char[] buffer = new char[4096];
                        int b;
                        while ((b = reader.read(buffer)) > -1) {
                            sb.append(buffer, 0, b);
                        }
                        reader.close();

                        errorContent = sb.toString();
                    } catch (IOException e) {
                        errorContent = "Unable to retrieve content: " + e.getMessage();
                    }
                }
            } else {
                try {
                    errorStream.close();
                } catch (IOException e) {
                }
            }

            if (responseStream != null) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                }
            }

            return;
        }

        // get the stream length
        String lengthStr = getHeader("Content-Length");
        if (lengthStr != null) {
            try {
                length = new BigInteger(lengthStr);
            } catch (NumberFormatException e) {
            }
        }

        if (stream == null || BigInteger.ZERO.equals(length) || responseCode == 204) {
            hasResponseStream = false;
        } else {
            stream = new BufferedInputStream(stream, 64 * 1024);
            try {
                stream.mark(2);
                if (stream.read() == -1) {
                    hasResponseStream = false;
                } else {
                    stream.reset();
                    hasResponseStream = true;
                }
            } catch (IOException ioe) {
                throw new CmisConnectionException("IO exception!", ioe);
            }

            if (hasResponseStream) {
                String encoding = getContentEncoding();
                if (encoding != null) {
                    if (encoding.toLowerCase().trim().equals("gzip")) {
                        // if the stream is gzip encoded, decode it
                        length = null;
                        try {
                            stream = new GZIPInputStream(stream, 4096);
                        } catch (IOException e) {
                            errorContent = e.getMessage();
                            stream = null;
                            try {
                                responseStream.close();
                            } catch (IOException ec) {
                            }
                        }
                    } else if (encoding.toLowerCase().trim().equals("deflate")) {
                        // if the stream is deflate encoded, decode it
                        length = null;
                        stream = new InflaterInputStream(stream, new Inflater(true), 4096);
                    }
                }

                String transferEncoding = getContentTransferEncoding();
                if ((stream != null) && (transferEncoding != null)
                        && (transferEncoding.toLowerCase().trim().equals("base64"))) {
                    // if the stream is base64 encoded, decode it
                    length = null;
                    stream = new Base64.InputStream(stream);
                }
            }
        }
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        List<String> list = headers.get(name.toLowerCase(Locale.US));
        if ((list == null) || (list.isEmpty())) {
            return null;
        }

        return list.get(0);
    }

    public String getContentTypeHeader() {
        return getHeader("Content-Type");
    }

    public BigInteger getContentLengthHeader() {
        String lengthStr = getHeader("Content-Length");
        if (lengthStr == null) {
            return null;
        }

        try {
            return new BigInteger(lengthStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLocactionHeader() {
        return getHeader("Location");
    }

    public String getContentLocactionHeader() {
        return getHeader("Content-Location");
    }

    public String getContentTransferEncoding() {
        return getHeader("Content-Transfer-Encoding");
    }

    public String getContentEncoding() {
        return getHeader("Content-Encoding");
    }

    public String getCharset() {
        return charset;
    }

    public BigInteger getContentLength() {
        return length;
    }

    public boolean hasResponseStream() {
        return hasResponseStream;
    }

    public InputStream getStream() {
        return stream;
    }

    public String getErrorContent() {
        return errorContent;
    }
}