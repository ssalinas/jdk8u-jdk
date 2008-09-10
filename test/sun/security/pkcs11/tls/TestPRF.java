/*
 * Copyright 2005-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/**
 * @test
 * @bug 6316539 6345251
 * @summary Basic known-answer-test for TlsPrf
 * @author Andreas Sterbenz
 * @library ..
 */

import java.io.*;
import java.util.*;

import java.security.Security;
import java.security.Provider;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import javax.crypto.spec.*;

import sun.security.internal.spec.*;

public class TestPRF extends PKCS11Test {

    private static int PREFIX_LENGTH = "prf-output: ".length();

    public static void main(String[] args) throws Exception {
        main(new TestPRF());
    }

    public void main(Provider provider) throws Exception {
        if (provider.getService("KeyGenerator", "SunTlsPrf") == null) {
            System.out.println("Provider does not support algorithm, skipping");
            return;
        }

        InputStream in = new FileInputStream(new File(BASE, "prfdata.txt"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        int n = 0;
        int lineNumber = 0;

        byte[] secret = null;
        String label = null;
        byte[] seed = null;
        int length = 0;
        byte[] output = null;

        while (true) {
            String line = reader.readLine();
            lineNumber++;
            if (line == null) {
                break;
            }
            if (line.startsWith("prf-") == false) {
                continue;
            }

            String data = line.substring(PREFIX_LENGTH);
            if (line.startsWith("prf-secret:")) {
                secret = parse(data);
            } else if (line.startsWith("prf-label:")) {
                label = data;
            } else if (line.startsWith("prf-seed:")) {
                seed = parse(data);
            } else if (line.startsWith("prf-length:")) {
                length = Integer.parseInt(data);
            } else if (line.startsWith("prf-output:")) {
                output = parse(data);

                System.out.print(".");
                n++;

                KeyGenerator kg = KeyGenerator.getInstance("SunTlsPrf", provider);
                SecretKey inKey;
                if (secret == null) {
                    inKey = null;
                } else {
                    inKey = new SecretKeySpec(secret, "Generic");
                }
                TlsPrfParameterSpec spec = new TlsPrfParameterSpec(inKey, label, seed, length);
                SecretKey key;
                try {
                    kg.init(spec);
                    key = kg.generateKey();
                } catch (Exception e) {
                    if (secret == null) {
                        // This fails on Solaris, but since we never call this
                        // API for this case in JSSE, ignore the failure.
                        // (SunJSSE uses the CKM_TLS_KEY_AND_MAC_DERIVE mechanism)
                        System.out.print("X");
                        continue;
                    }
                    System.out.println();
                    throw new Exception("Error on line: " + lineNumber, e);
                }
                byte[] enc = key.getEncoded();
                if (Arrays.equals(output, enc) == false) {
                    System.out.println();
                    System.out.println("expected: " + toString(output));
                    System.out.println("actual:   " + toString(enc));
                    throw new Exception("mismatch line: " + lineNumber);
                }
            } else {
                throw new Exception("Unknown line: " + line);
            }
        }
        if (n == 0) {
            throw new Exception("no tests");
        }
        in.close();
        System.out.println();
        System.out.println("OK: " + n + " tests");
    }

}
