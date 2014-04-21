/*
 * Copyright (C) 2009 by Robert Stewart <robert@wombatnation.com>
 * Use and distribution licensed under the
 * GNU Lesser General Public License (LGPL) version 2.1.
 * See the COPYING file in the parent directory for full text.
 */
package org.gearman.example;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.gearman.client.GearmanJobResult;
import org.gearman.client.GearmanJobResultImpl;
import org.gearman.util.ByteArrayBuffer;
import org.gearman.util.ByteUtils;
import org.gearman.worker.AbstractGearmanFunction;

/**
 * The data passed to DigestFunction should contain two parameters separated by
 * a null byte.
 * <ol>
 * <li>name of a digest algorithm implemented by
 * {@link java.security.MessageDigest}
 * <li>data to digest
 * </ol>
 */
public class DigestFunction extends AbstractGearmanFunction {


    @Override
    public GearmanJobResult executeFunction() {
        // First param is algorithm. Second is the data to digest.
        ByteArrayBuffer bab = new ByteArrayBuffer((byte[]) this.data);
        List<byte[]> params = bab.split(new byte[]{'\0'});
        GearmanJobResult jr = null;
        if (params.size() != 2) {
            String msg = "Data to digest should be preceded by name " +
                    "of an algorithm";
            jr = new GearmanJobResultImpl(this.jobHandle, false, new byte[0],
                    new byte[0], ByteUtils.toUTF8Bytes(msg), 0, 0);
            return jr;
        }
        String algorithm = ByteUtils.fromUTF8Bytes(params.get(0));
        byte[] digestData = params.get(1);

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(digestData);
            jr = new GearmanJobResultImpl(this.jobHandle, false, digest,
                    new byte[0], new byte[0], 0, 0);
        } catch (NoSuchAlgorithmException e) {
            String msg = "Unsupported digest algorithm " + algorithm;
            jr = new GearmanJobResultImpl(this.jobHandle, false, new byte[0],
                    new byte[0], ByteUtils.toUTF8Bytes(msg), 0, 0);
        }
        return jr;
    }
}
