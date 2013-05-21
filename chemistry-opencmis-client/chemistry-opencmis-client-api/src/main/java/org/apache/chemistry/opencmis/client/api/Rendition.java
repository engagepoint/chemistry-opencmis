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
package org.apache.chemistry.opencmis.client.api;

import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RenditionData;

/**
 * Rendition.
 * 
 * @cmis 1.0
 */
public interface Rendition extends RenditionData {

    /**
     * Returns the size of the rendition in byte if available.
     */
    long getLength();

    /**
     * Returns the height in pixels if the rendition is an image.
     */
    long getHeight();

    /**
     * Returns the width in pixels if the rendition is an image.
     */
    long getWidth();

    /**
     * Returns the rendition document if the rendition is a stand-alone
     * document.
     */
    Document getRenditionDocument();

    /**
     * Returns the rendition document using the provides
     * {@link OperationContext} if the rendition is a stand-alone document.
     */
    Document getRenditionDocument(OperationContext context);

    /**
     * Returns the content stream of the rendition.
     */
    ContentStream getContentStream();
}
