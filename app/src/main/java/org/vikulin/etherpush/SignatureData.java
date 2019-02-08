package org.vikulin.etherpush;

import java.util.Arrays;

public class SignatureData {
	
    private final byte v;
    private final byte[] r;
    private final byte[] s;

    public SignatureData(byte v, byte[] r, byte[] s) {
        this.v = v;
        this.r = r;
        this.s = s;
    }

    public byte getV() {
        return v;
    }

    public byte[] getR() {
        return r;
    }

    public byte[] getS() {
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SignatureData that = (SignatureData) o;

        if (v != that.v) return false;
        if (!Arrays.equals(r, that.r)) return false;
        return Arrays.equals(s, that.s);

    }

    @Override
    public int hashCode() {
        int result = (int) v;
        result = 31 * result + Arrays.hashCode(r);
        result = 31 * result + Arrays.hashCode(s);
        return result;
    }

}
