/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springaicommunity.agentcore.codeinterpreter;

import java.util.Arrays;
import java.util.Base64;

/**
 * Represents a file produced by code execution.
 *
 * @param mimeType MIME type of the file (e.g., "image/png", "application/pdf")
 * @param data raw bytes of the file content (defensively copied)
 * @param name optional filename hint
 * @author Yuriy Bezsonov
 */
public record GeneratedFile(String mimeType, byte[] data, String name) {

	/**
	 * Canonical constructor with defensive copy of data array.
	 */
	public GeneratedFile {
		data = data != null ? Arrays.copyOf(data, data.length) : new byte[0];
	}

	/**
	 * Returns a copy of the data array to prevent mutation.
	 * @return copy of the file data
	 */
	@Override
	public byte[] data() {
		return Arrays.copyOf(data, data.length);
	}

	/**
	 * Check if this file is an image.
	 * @return true if MIME type starts with "image/"
	 */
	public boolean isImage() {
		return mimeType != null && mimeType.startsWith("image/");
	}

	/**
	 * Check if this file is a text file.
	 * @return true if MIME type indicates text content
	 */
	public boolean isText() {
		if (mimeType == null) {
			return false;
		}
		return mimeType.startsWith("text/") || mimeType.equals("application/json")
				|| mimeType.equals("application/xml");
	}

	/**
	 * Convert to data URL for embedding in HTML/markdown.
	 * @return data URL string (e.g., "data:image/png;base64,...")
	 */
	public String toDataUrl() {
		// Access internal field directly to avoid defensive copy overhead
		return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(this.data);
	}

	/**
	 * Get the size of the file data in bytes.
	 * @return size in bytes
	 */
	public int size() {
		return data.length;
	}

}
