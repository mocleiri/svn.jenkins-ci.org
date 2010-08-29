/*
 * The MIT License
 * 
 * Copyright (c) 2010 Steven G. Brown
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.timestamper;

import hudson.Extension;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Build wrapper that decorates the build's logger to insert a
 * {@link TimestampNote} on each output line.
 * 
 * @author Steven G. Brown
 */
public final class TimestamperBuildWrapper extends BuildWrapper {

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) {
    return new TimestamperOutputStream(logger);
  }

  /**
   * Output stream that writes each line to the provided delegate output stream
   * after inserting a {@link TimestampNote}.
   */
  private static class TimestamperOutputStream extends
      LineTransformationOutputStream {

    /**
     * The delegate output stream.
     */
    private final OutputStream delegate;

    /**
     * Create a new {@link TimestamperOutputStream}.
     * 
     * @param delegate
     *          the delegate output stream
     */
    private TimestamperOutputStream(OutputStream delegate) {
      this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void eol(byte[] b, int len) throws IOException {
      new TimestampNote(System.currentTimeMillis()).encodeTo(delegate);
      delegate.write(b, 0, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
      super.close();
      delegate.close();
    }
  }

  /**
   * Registers {@link TimestamperBuildWrapper} as a {@link BuildWrapper}.
   */
  @Extension
  public static final class DescriptorImpl extends BuildWrapperDescriptor {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
      return Messages.DisplayName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
      return true;
    }
  }
}
