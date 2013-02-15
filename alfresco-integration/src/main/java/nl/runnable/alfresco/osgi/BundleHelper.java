/*
Copyright (c) 2012, Runnable
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
 * Neither the name of Runnable nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.runnable.alfresco.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Helper for {@link Bundle} operations. Takes care of logging and exception handling.
 * 
 * @author Laurens Fridael
 * 
 */
class BundleHelper {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void startBundle(final Bundle bundle) {
		Assert.notNull(bundle, "Bundle cannot be null.");

		if (isFragmentBundle(bundle) == false) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Starting Bundle. ID: {}, Symbolic Name: {}, Version: {}",
							new Object[] { bundle.getBundleId(), bundle.getSymbolicName(), bundle.getVersion() });
				}
				bundle.start();
			} catch (final BundleException e) {
				logger.warn("Error starting Bundle", e);
			}
		}
	}

	public void stopBundle(final Bundle bundle) {
		Assert.notNull(bundle, "Bundle cannot be null.");

		if (isFragmentBundle(bundle) == false) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Stopping Bundle. ID: {}, Symbolic Name: {}, Version: {}",
							new Object[] { bundle.getBundleId(), bundle.getSymbolicName(), bundle.getVersion() });
				}
				bundle.stop();
			} catch (final BundleException e) {
				logger.warn("Error stopping Bundle", e);
			}
		}
	}

	public void restartBundle(final Bundle bundle) {
		Assert.notNull(bundle, "Bundle cannot be null.");

		if (isFragmentBundle(bundle) == false) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Restarting Bundle. ID: {}, Symbolic Name: {}, Version: {}",
							new Object[] { bundle.getBundleId(), bundle.getSymbolicName(), bundle.getVersion() });
				}
				bundle.stop();
				bundle.start();
			} catch (final BundleException e) {
				logger.warn("Error restarting Bundle", e);
			}
		}
	}

	public boolean isFragmentBundle(final Bundle bundle) {
		Assert.notNull(bundle, "Bundle cannot be null.");
		return bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
	}
}
