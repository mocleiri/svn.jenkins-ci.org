/*******************************************************************************
* Copyright (c) 2009 Thales Corporate Services SAS                             *
* Author : Gregory Boissinot                                                   *
*                                                                              *
* Permission is hereby granted, free of charge, to any person obtaining a copy *
* of this software and associated documentation files (the "Software"), to deal*
* in the Software without restriction, including without limitation the rights *
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
* copies of the Software, and to permit persons to whom the Software is        *
* furnished to do so, subject to the following conditions:                     *
*                                                                              *
* The above copyright notice and this permission notice shall be included in   *
* all copies or substantial portions of the Software.                          *
*                                                                              *
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
* THE SOFTWARE.                                                                *
*******************************************************************************/

package com.thalesgroup.hudson.plugins.xunit;

import java.io.Serializable;

public class TypeConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;  // The test tool name

    private final String label; // The label tool name
    
    private String pattern = null; // File name pattern

    public TypeConfig(String name, String label) {
        this.name = name;
        this.label=label;
    }

    public String getName() {
        return name;
    }

    public String getPattern() {
        return pattern;
    }
    
    public String getLabel() {
        return label;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isFill() {
        if (pattern== null){
            return false;
        }

        if (pattern.isEmpty()){
            return false;
        }

        return pattern.trim().length()!=0;        
    }
}
