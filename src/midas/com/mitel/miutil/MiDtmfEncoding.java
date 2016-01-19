/*
 * MiDtmfEncoding.java
 *
 * Created on April 27, 2006, 9:34 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.mitel.miutil;

/**
 * DTMF encoding scheme.
 * @author haiv
 */
public class MiDtmfEncoding
{
	/**
	 * DTMF encoding types: Mitel proprietary (deprecated), or RFC2833
	 */
	public static enum EncodingType
	{
		/**
		 * Mitel DTMF encoding sheme (deprecated)
		 */
		DTMF_MITEL,
		/**
		 * DTMF encoding scheme RFC2833
		 */
		DTMF_RFC2833
	}
	
	/**
	 * Creates a new instance of MiDtmfEncoding representing the Mitel RFC2833 encoding scheme.
	 */
	public MiDtmfEncoding()
	{
		 m_DtmfEncoding = EncodingType.DTMF_RFC2833;
		 m_DtmfPayloadType = 101; // Mitel's payload type for RFC2833.
		 m_UpperBitsValid = false;
         m_DtmfSamplesInStreamEnabled = false;
		 m_DtmfInBandEnabled = false;
	}

	/**
	 * Creates a new instance of MiDtmfEncoding, defaulting "samplesInStream" to false for backward compatibility
	 * @param dtmfEncoding The DTMF encoding type, either DTMF_MITEL, or DTMF_RFC2833.
	 * @param dtmfPayloadType Payload type, for DTMF_MITEL, this is hard-coded to 0x60, for RFC2833 this is in the range of 0x60-0x7f, 0x00.
	 */
	public MiDtmfEncoding(EncodingType dtmfEncoding, short dtmfPayloadType)
	{
		m_DtmfEncoding = dtmfEncoding;
		m_DtmfPayloadType = dtmfPayloadType;
		m_UpperBitsValid = false;
		m_DtmfInBandEnabled = false;
        m_DtmfSamplesInStreamEnabled = false;
	}

	/**
	 * Creates a new instance of MiDtmfEncoding
	 * @param dtmfEncoding The DTMF encoding type, either DTMF_MITEL, or DTMF_RFC2833.
	 * @param dtmfPayloadType Payload type, for DTMF_MITEL, this is hard-coded to 0x60, for RFC2833 this is in the range of 0x60-0x7f, 0x00.
	 * @param inBandToneEnabled Flag indicating whether in-band DTMF (RFC2833/Mitel) is enabled. Looks like this is mutually exclusive with samplesInStreamEnabled.
     * @param samplesInStreamEnabled Flag to indicate that DTMF samples are in the stream. Looks like this is mutually exclusive with inBandToneEnabled.
	 */
	public MiDtmfEncoding(EncodingType dtmfEncoding, short dtmfPayloadType, boolean inBandToneEnabled, boolean samplesInStreamEnabled)
	{
		m_DtmfEncoding = dtmfEncoding;
		m_DtmfPayloadType = dtmfPayloadType;
		m_UpperBitsValid = true;
		m_DtmfInBandEnabled = inBandToneEnabled;
        m_DtmfSamplesInStreamEnabled = samplesInStreamEnabled;
	}

	/**
	 * Extracts the MiNET tone encoding flag (comes from an open RT/TX request) into a DTMF encoding object.
	 * @param minetToneEncoding The MiNET encoding flag.
	 */
	public MiDtmfEncoding(int minetToneEncoding)
	{
		if(0 != (minetToneEncoding & mask_DTMF_toneControl_useRFC2833))
		{
			m_DtmfEncoding = EncodingType.DTMF_RFC2833;
			m_DtmfPayloadType = (short)((minetToneEncoding >> shift_RTP_payload_value) & 0xff);
		}
		else
		{
			m_DtmfEncoding = EncodingType.DTMF_MITEL;
			m_DtmfPayloadType = (short)((minetToneEncoding >> shift_RTP_payload_value) & 0xff); 
		}
		m_UpperBitsValid = (0 != (minetToneEncoding & mask_DTMF_toneControl_DTMF_upperBitsValid));
		if(m_UpperBitsValid)
		{
            m_DtmfSamplesInStreamEnabled
                    = ((minetToneEncoding & mask_DTMF_toneControl_samplesInStreamEnable) != 0);
            m_DtmfInBandEnabled
                    = ((minetToneEncoding & mask_DTMF_toneControl_inbandToneEnable) != 0);
        }
        else
        {
            m_DtmfInBandEnabled = false;
            m_DtmfSamplesInStreamEnabled = false;
        }
	}

    /**
     * Answers an int representing MinetToneEncoding as used in the open rx/tx messages.
     * i.e. Performs the opposite function as the constructor:
     *      public MiDtmfEncoding(int minetToneEncoding)
     * @return int The encoded int
     */
    public int intValue()
	{
		// In-band tone encoding
		int minetToneEncoding = 0;
		if(m_DtmfEncoding == MiDtmfEncoding.EncodingType.DTMF_RFC2833)
		{
			minetToneEncoding |=
                    (MiDtmfEncoding.mask_DTMF_toneControl_useRFC2833
                        | (m_DtmfPayloadType << MiDtmfEncoding.shift_RTP_payload_value));
		}
		else
		{
			minetToneEncoding |= (m_DtmfPayloadType << MiDtmfEncoding.shift_RTP_payload_value) ; 
		}
		
		if(m_UpperBitsValid)
		{
			minetToneEncoding |= mask_DTMF_toneControl_DTMF_upperBitsValid;
		}

        if (m_DtmfSamplesInStreamEnabled)
        {
            minetToneEncoding |= MiDtmfEncoding.mask_DTMF_toneControl_samplesInStreamEnable ;
        }

		if(m_DtmfInBandEnabled)
		{
			minetToneEncoding |= MiDtmfEncoding.mask_DTMF_toneControl_inbandToneEnable;
		}

        return minetToneEncoding ;
    }

	public MiDtmfEncoding(MiDtmfEncoding aCopy)
	{
		m_DtmfEncoding = aCopy.m_DtmfEncoding;
		m_DtmfPayloadType = aCopy.m_DtmfPayloadType;
		m_UpperBitsValid = aCopy.m_UpperBitsValid;
        m_DtmfSamplesInStreamEnabled = aCopy.m_DtmfSamplesInStreamEnabled;
		m_DtmfInBandEnabled = aCopy.m_DtmfInBandEnabled;
	}

	/**
	 * Retrieves the effective payload type, almost the same as m_DtmfPayloadType, with
	 * the exception that 0 is translated to 0x60 (see open TX in MiNET spec).
	 * @return The payload type as described above.
	 */
	public short payloadType()
	{
		return (m_DtmfPayloadType == 0?0x60:m_DtmfPayloadType);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof MiDtmfEncoding)
		{
			MiDtmfEncoding rhs = (MiDtmfEncoding)obj;
			return (m_DtmfInBandEnabled == rhs.m_DtmfInBandEnabled)
				&&(m_DtmfEncoding == rhs.m_DtmfEncoding)
                &&(m_DtmfSamplesInStreamEnabled == rhs.m_DtmfSamplesInStreamEnabled)
				&&(m_DtmfPayloadType == rhs.m_DtmfPayloadType);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		// This code was auto generated by Netbeans
		int hash = 7;
		hash = 17 * hash + (this.m_DtmfEncoding != null ? this.m_DtmfEncoding.hashCode() : 0);
		hash = 17 * hash + this.m_DtmfPayloadType;
		hash = 17 * hash + (this.m_DtmfInBandEnabled ? 1 : 0);
		return hash;
	}

	@Override
	public String toString()
	{
		StringBuilder retValue = new StringBuilder("(DtmfEncoding");
		retValue.append(" Encoding=\"").append(m_DtmfEncoding).append('"');
		retValue.append(" Payload=\"").append(payloadType()).append('"');
		if(m_UpperBitsValid)
		{
			retValue.append(" DtmfSamplesInStreamEnabled=\"").append(m_DtmfSamplesInStreamEnabled).append('"');
			retValue.append(" InBandToneEnabled=\"").append(m_DtmfInBandEnabled).append('"');
		}
		return retValue.append(")").toString();
	}
	
	/**
	 * Encoding type, either DTMF_MITEL (deprecated), or DTMF_RFC2833 (default).
	 */
	public final EncodingType m_DtmfEncoding;
	/**
	 * DTMF payload type, for DTMF_MITEL this is 0x60, for DTMF_RFC2833, Mitel uses 101. Zero actually means 0x60 for backward compatible.
	 * Clients shouldn't access this directly but use payloadType() instead.
	 */
	public final short m_DtmfPayloadType;
	/**
	 * Determines whether m_DtmfSamplesInStreamEnabled & m_DtmfInBandEnabled are valid, i.e. this is for a TX stream.
	 */
	public final boolean m_UpperBitsValid;
    /**
     * Only relevant for TX. DTMF samples in stream enabled; true indicates TX DTMF tones in stream
	 * This appears to be mutually exclusive to m_DtmfInBandEnabled.
     */
    public final boolean m_DtmfSamplesInStreamEnabled;
	/**
	 * Only relevant for TX. Whether DTMF in-band (RFC2833/Mitel) is enabled.
	 * This appears to be mutually exclusive to m_DtmfSamplesInStreamEnabled.
	 */
	public final boolean m_DtmfInBandEnabled;

    /**
     * Masks for the DTMF tone encoding byte
     */
    private static final int mask_DTMF_toneControl_useRFC2833 = 0x01;
    private static final int mask_DTMF_toneControl_DTMF_upperBitsValid = 0x02;
    private static final int mask_DTMF_toneControl_samplesInStreamEnable = 0x04;
    private static final int mask_DTMF_toneControl_inbandToneEnable = 0x08;

    private static final int shift_RTP_payload_value = 8 ;  // shift 8 bits to move to the second byte
}
