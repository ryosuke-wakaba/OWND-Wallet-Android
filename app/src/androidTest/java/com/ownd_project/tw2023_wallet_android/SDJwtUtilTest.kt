package com.ownd_project.tw2023_wallet_android

import com.ownd_project.tw2023_wallet_android.utils.SDJwtUtil
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test

class SDJwtUtilTest {
    @Test
    fun testDivideSDJwt() {
        val sdJwt =
            "eyJ0eXAiOiJzZCtqd3QiLCJhbGciOiJFUzI1NksiLCJ4NWMiOlsiTUlJQzFqQ0NBYjZnQXdJQkFnSVVXd3ZhU1RyMjgxV3o1R0wrQjNsK1ZONW1YWm93RFFZSktvWklodmNOQVFFTEJRQXdKVEVqTUNFR0ExVUVBd3dhVkdWemRDQkRaWEowYVdacFkyRjBaU0JCZFhSb2IzSnBkSGt3SGhjTk1qTXhNVEE1TURFMU5UTXpXaGNOTWpReE1UQTRNREUxTlRNeldqQnFNUXN3Q1FZRFZRUUdFd0pLVURFU01CQUdBMVVFQ0F3SjVwMng1THFzNllPOU1SSXdFQVlEVlFRSERBbm1sckRscnIvbGpMb3hIVEFiQmdOVkJBb01GT2FncXVXOGorUzhtdWVrdmtSaGRHRlRhV2R1TVJRd0VnWURWUVFEREF0a1lYUmhjMmxuYmk1cWNEQldNQkFHQnlxR1NNNDlBZ0VHQlN1QkJBQUtBMElBQk0yNW53b21QdlZ1dkdzOGdnZVU2dnUzMmQrK0I3eWJ5MWI1R0JUbkcraFJxd1hnL0xZTFg0RldzQ0htZXFHZzFVZzA1MEhOTHM5WVBqMkdaVEprWVFLamdZWXdnWU13RmdZRFZSMFJCQTh3RFlJTFpHRjBZWE5wWjI0dWFuQXdIUVlEVlIwT0JCWUVGRVVTZUpNOEtTcXg1M0c0ZVUxQjQvMW5qQUhZTUVvR0ExVWRJd1JETUVHaEthUW5NQ1V4SXpBaEJnTlZCQU1NR2xSbGMzUWdRMlZ5ZEdsbWFXTmhkR1VnUVhWMGFHOXlhWFI1Z2hRdHdBL3hzMmxxbzFTRUJXTlhtbWVFaGJqdXF6QU5CZ2txaGtpRzl3MEJBUXNGQUFPQ0FRRUFIZFIzdXV0b0MrUlE3NTBNY0x6OWVGdHpFcnVZa0dVMGFDbkNNenBNSjNITVc2M3BPS0ZWVmhwTnhpcnorcG0vRnBEd0FjTFQxamdLdmRiSDRjYWk4b1RmZDg0R3VFbGR4T3lOWVZySXlia0pPSmxhMXRabG9XNldqR2ZLVlk4WUFhS3dIVlFCY3dhL3N0ZDE4ajNnN0NBL2g5VjR3S1V0UFlMS05vYkFPay9DU0QyQkNIU2R0NDlNUmRrZ3lpZ2p4aDY1NHFrL0RJc3JLejZWVVI3L1VQdnVHdXdQdFpoaElzLzg5T29OWjJ5dk1LQ2ZmTUdITEw5VEtlR0dWVmY5b3pWeFYvbE5ibmVYbUdEMmt2WjF6RmJSd2FZQ213NERjSUFZTGlqMjluYWhib1k4MGhkdDg2SFplNDJlc1FTQkRCemFUeUE0RXZYSDVsNUFVdFNIbWc9PSIsIk1JSUMwVENDQWJrQ0ZDM0FEL0d6YVdxalZJUUZZMWVhWjRTRnVPNnJNQTBHQ1NxR1NJYjNEUUVCQ3dVQU1DVXhJekFoQmdOVkJBTU1HbFJsYzNRZ1EyVnlkR2xtYVdOaGRHVWdRWFYwYUc5eWFYUjVNQjRYRFRJek1URXdPVEF4TkRrME5Wb1hEVEkwTVRFd09EQXhORGswTlZvd0pURWpNQ0VHQTFVRUF3d2FWR1Z6ZENCRFpYSjBhV1pwWTJGMFpTQkJkWFJvYjNKcGRIa3dnZ0VpTUEwR0NTcUdTSWIzRFFFQkFRVUFBNElCRHdBd2dnRUtBb0lCQVFEQ2oyZzFtN1lRWmIxTE1PbE15MnpyT0NnOWNBRXpySzdyY3R5bUZGZDlyNzdKTU9pMWMzbnpJbTZaV2Vtd1NOeEdZMnlVU0IrQ05ISkRRK1c5dk8yTS85RkZ2dUt4TWZWQ0RERUJWMXc5cmtOZGpJR2N2aExBNlZqaHhvQU4wWDRWUm04cHpXN0tLc3I5UE1yMkhaVmJxb3JMVG5Ua0M1YUhob3FWY0xlL09Gbm00TnpVMDJCOXhlY2Fhb3FhalBBWEhsdEZ0RCtEVktFNm1RdVJ0RDhLT0lSaFBmSDlVdW9yT1lWMmVtTEt3MWI3TUZNNU84SUVUY0tEMnRhemNIUlFiRmlvLzZWU1lYaWtCekhJOWJ0dGZkMnFtbVRtVE9MSWhGc2dUYm5acWxHYzltWWIzSEEybVN5blhnMC9OemRPNE1zRi9iaEZtd1N2Q3hCUENZbVJBZ01CQUFFd0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFMekhCZXZPeGJWWEhEOGlIZGhqTHFiVURpWmtPTzg0VU5JRk9JY2VxNEo2ODVxeEphSlo2NVNKVjZmM2d3YlJhM0JDZUpzTEdTa3RjS0o1a2NOOFMzc0ZBQm1Nb1lTQm4vS0xHMjY3Mm9PQ21kYVpWL25vdHVoeDJwd3FyVHROLzVadzE0VWxBNzhiSDhzdUN6SEtwZFNiR1I3clpRbVh5QWtJbDJWVUdPRkNrbFdLeWxzWEZZY29UL2p3Z2dUbGYyNnp3Ynpsa2pJRkZZK3NYcjBuOGdDYWw5bzQwZUhhTldBc2tOZEEyQ3ZpcXg1RklsdG83L09LMGxyS2lKeGtOSThFY0JCUVJlMG1EbnpqNlFYeFlMT3FpaEMxb3dRdWtVY05GSGR5YTRsdlZYMWYxaHgyUkRMWUcycWd2WmVQemY2R3lSSDBtVVM5YXNwNmNCUjhBc2M9Il19.eyJjbmYiOnsia3R5IjoiRUMiLCJjcnYiOiJzZWNwMjU2azEiLCJ4IjoidFZ3VHBCRTB2QVlpdnZXOGlseWtnYWQ4RFM3cHFVcUNiUU9JQ2gwU29nbyIsInkiOiJTalZIbS1QX0NrWUVteURDR2p5OUlWc3Rsb0YyVDB3cUZVREtMeUVER1J3In0sImlhdCI6MCwidHlwZSI6IiIsImlzcyI6IiIsIl9zZCI6WyIySTdTcFVQcE11R1VlMjVDM21wN0ExRG10R1NiQnBnN0Qwb2dRbHI0VWZFIiwicmdyVW42M0ZhblJkTGlqc3pPUEkyN0FOUHA0NnVpV2kxRFdYX3k0VlRqWSIsIndpT2ZDdTUwYmpURldoZkktWGJmNGFlR19WSi0zOWdYV3BXQV9ILVdEUG8iLCIwSFdCUEZmVTFJSjhZYXFsUGdJeEJHQ3QtNlUtTjJJRWw3X1JzMTY5OGdFIiwiOEI1M1BENzdHMkMyajFnd180V0o4a1pTalY3S0I5MjNodVhpMVBWeGpmYyIsIlN2VXdfSFh5cVNqcUxLTHN6dG5GcHlEMkRwb1JsczYybG44SzVMcFUxS0EiLCJfU0lKLUtqTWxzaGtnbkFtYmVxM0pPenduVGhNU0hzZ08wTW1BQXJadHBnIl19.dNzrFlXB98jAUxzMxWG6hgbXb59fsX_Ib0tpYOOh_6d4qsxYIfh8_VkcrcSZmCmVr-p8tjXk_PfEdM25XgP9fA~WyJjRXdQVDU2NmNpSVhMb1VUIiwidmVyaWZpZWRfYXQiLDBd~WyJteE0xRjdOQzBmY1hDdERnIiwibGFzdF9uYW1lIiwiIl0~WyJXYmRLbTVEMzYwZVNNczltIiwiZmlyc3RfbmFtZSIsIiJd~WyJuR05ldXRwTjY4UEFRdGVvIiwiaXNfb3JkZXJfdGhhbl8xNSIsZmFsc2Vd~WyJ0TGljVHJwdWh2c2ZrMmlSIiwiaXNfb3JkZXJfdGhhbl8xOCIsZmFsc2Vd~WyJjYmFBSGY4eUl6RXFack8xIiwiaXNfb3JkZXJfdGhhbl8yMCIsZmFsc2Vd~WyJhUEpQcHhTb2dIMkpKYUhMIiwiaXNfb3JkZXJfdGhhbl82NSIsZmFsc2Vd~"
        val result = SDJwtUtil.divideSDJwt(sdJwt)

        assertNotNull(result.issuerSignedJwt)
        assertNotNull(result.disclosures)

        println(result.issuerSignedJwt)

        val sdJwt2 =
            "eyJhbGciOiAiRVMyNTYifQ.eyJfc2QiOiBbIjA5dktySk1PbHlUV00wc2pwdV9wZE9CVkJRMk0xeTNLaHBINTE1blhrcFkiLCAiMnJzakdiYUMwa3k4bVQwcEpyUGlvV1RxMF9kYXcxc1g3NnBvVWxnQ3diSSIsICJFa084ZGhXMGRIRUpidlVIbEVfVkNldUM5dVJFTE9pZUxaaGg3WGJVVHRBIiwgIklsRHpJS2VpWmREd3BxcEs2WmZieXBoRnZ6NUZnbldhLXNONndxUVhDaXciLCAiSnpZakg0c3ZsaUgwUjNQeUVNZmVadTZKdDY5dTVxZWhabzdGN0VQWWxTRSIsICJQb3JGYnBLdVZ1Nnh5bUphZ3ZrRnNGWEFiUm9jMkpHbEFVQTJCQTRvN2NJIiwgIlRHZjRvTGJnd2Q1SlFhSHlLVlFaVTlVZEdFMHc1cnREc3JaemZVYW9tTG8iLCAiamRyVEU4WWNiWTRFaWZ1Z2loaUFlX0JQZWt4SlFaSUNlaVVRd1k5UXF4SSIsICJqc3U5eVZ1bHdRUWxoRmxNXzNKbHpNYVNGemdsaFFHMERwZmF5UXdMVUs0Il0sICJpc3MiOiAiaHR0cHM6Ly9leGFtcGxlLmNvbS9pc3N1ZXIiLCAiaWF0IjogMTY4MzAwMDAwMCwgImV4cCI6IDE4ODMwMDAwMDAsICJ0eXBlIjogIklkZW50aXR5Q3JlZGVudGlhbCIsICJfc2RfYWxnIjogInNoYS0yNTYiLCAiY25mIjogeyJqd2siOiB7Imt0eSI6ICJFQyIsICJjcnYiOiAiUC0yNTYiLCAieCI6ICJUQ0FFUjE5WnZ1M09IRjRqNFc0dmZTVm9ISVAxSUxpbERsczd2Q2VHZW1jIiwgInkiOiAiWnhqaVdXYlpNUUdIVldLVlE0aGJTSWlyc1ZmdWVjQ0U2dDRqVDlGMkhaUSJ9fX0.nIneu9ghAY_pjnfAczLZe80xN2Z-jjxs42MOx8UVZtCT4ACFVW8RMdBANSwjfRBD1Xag6vnayC6HUnBvkVWlMQ~WyJRZ19PNjR6cUF4ZTQxMmExMDhpcm9BIiwgImFkZHJlc3MiLCB7InN0cmVldF9hZGRyZXNzIjogIjEyMyBNYWluIFN0IiwgImxvY2FsaXR5IjogIkFueXRvd24iLCAicmVnaW9uIjogIkFueXN0YXRlIiwgImNvdW50cnkiOiAiVVMifV0~eyJhbGciOiAiRVMyNTYiLCAidHlwIjogImtiK2p3dCJ9.eyJub25jZSI6ICIxMjM0NTY3ODkwIiwgImF1ZCI6ICJodHRwczovL2V4YW1wbGUuY29tL3ZlcmlmaWVyIiwgImlhdCI6IDE2ODkwMTIzMzR9.3rROqnKkUNgiYfSwtmTXnXVMtSfb_RA2pVVvEclTihvi7MIZ-W_x3sVPVliJJIDdH4frgvxjxMWa__vCoTEbqQ"
        val result2 = SDJwtUtil.divideSDJwt(sdJwt2)
        assertNotNull(result2.issuerSignedJwt)
        assertNotNull(result2.disclosures)
        assertNotNull(result2.keyBindingJwt)
    }

    @Test
    fun testDivideSDJwt2() {
        val sdJwtWithoutDisclosureAndKBJ = "ISSUER_SIGNED_JWT~"
        val sdJwtWithoutDisclosureWithKBJ = "ISSUER_SIGNED_JWT~KBJ"
        val sdJwtWithDisclosureWithoutKBJ = "ISSUER_SIGNED_JWT~DISCLOSURE1~DISCLOSURE2~"
        val sdJwtWithDisclosureWithKBJ = "ISSUER_SIGNED_JWT~DISCLOSURE1~DISCLOSURE2~KBJ"

        val parts1 = SDJwtUtil.divideSDJwt(sdJwtWithoutDisclosureAndKBJ)
        val parts2 = SDJwtUtil.divideSDJwt(sdJwtWithoutDisclosureWithKBJ)
        val parts3 = SDJwtUtil.divideSDJwt(sdJwtWithDisclosureWithoutKBJ)
        val parts4 = SDJwtUtil.divideSDJwt(sdJwtWithDisclosureWithKBJ)

        // Issuer Signed Jwt
        assertEquals("ISSUER_SIGNED_JWT", parts1.issuerSignedJwt)
        assertEquals("ISSUER_SIGNED_JWT", parts2.issuerSignedJwt)
        assertEquals("ISSUER_SIGNED_JWT", parts3.issuerSignedJwt)
        assertEquals("ISSUER_SIGNED_JWT", parts4.issuerSignedJwt)

        // Disclosure
        assertTrue(parts1.disclosures.isEmpty())
        assertTrue(parts2.disclosures.isEmpty())
        assertTrue(parts3.disclosures.size == 2)
        assertTrue(parts3.disclosures[0] == "DISCLOSURE1")
        assertTrue(parts3.disclosures[1] == "DISCLOSURE2")
        assertTrue(parts4.disclosures.size == 2)
        assertTrue(parts4.disclosures[0] == "DISCLOSURE1")
        assertTrue(parts4.disclosures[1] == "DISCLOSURE2")

        // KBJWT
        assertNull(parts1.keyBindingJwt)
        assertNull(parts3.keyBindingJwt)
        assertNotNull(parts2.keyBindingJwt)
        assertNotNull(parts4.keyBindingJwt)
    }

    @Test
    fun testDecodeDisclosure() {
        val encodedDisclosure = listOf(
            "WyJjRXdQVDU2NmNpSVhMb1VUIiwidmVyaWZpZWRfYXQiLDBd",
            "WyJteE0xRjdOQzBmY1hDdERnIiwibGFzdF9uYW1lIiwiIl0",
            "WyJXYmRLbTVEMzYwZVNNczltIiwiZmlyc3RfbmFtZSIsIiJd",
            "WyJuR05ldXRwTjY4UEFRdGVvIiwiaXNfb3JkZXJfdGhhbl8xNSIsZmFsc2Vd",
            "WyJ0TGljVHJwdWh2c2ZrMmlSIiwiaXNfb3JkZXJfdGhhbl8xOCIsZmFsc2Vd",
            "WyJjYmFBSGY4eUl6RXFack8xIiwiaXNfb3JkZXJfdGhhbl8yMCIsZmFsc2Vd",
            "WyJhUEpQcHhTb2dIMkpKYUhMIiwiaXNfb3JkZXJfdGhhbl82NSIsZmFsc2Vd"
        )
        val result = SDJwtUtil.decodeDisclosure(encodedDisclosure)

        // 期待されるキーと値を検証
        assertEquals("verified_at", result.first().key)
        assertEquals("0", result.first().value)

        assertEquals("last_name", result[1].key)
        assertEquals("", result[1].value)

        assertEquals("first_name", result[2].key)
        assertEquals("", result[2].value)

        assertEquals("is_order_than_15", result[3].key)
        assertEquals("false", result[3].value)
    }

    @Test
    fun testDecodeIssuerSignedJwt() {
        val sdJwt =
            "eyJ0eXAiOiJzZCtqd3QiLCJhbGciOiJFUzI1NksiLCJ4NWMiOlsiTUlJQzFqQ0NBYjZnQXdJQkFnSVVXd3ZhU1RyMjgxV3o1R0wrQjNsK1ZONW1YWm93RFFZSktvWklodmNOQVFFTEJRQXdKVEVqTUNFR0ExVUVBd3dhVkdWemRDQkRaWEowYVdacFkyRjBaU0JCZFhSb2IzSnBkSGt3SGhjTk1qTXhNVEE1TURFMU5UTXpXaGNOTWpReE1UQTRNREUxTlRNeldqQnFNUXN3Q1FZRFZRUUdFd0pLVURFU01CQUdBMVVFQ0F3SjVwMng1THFzNllPOU1SSXdFQVlEVlFRSERBbm1sckRscnIvbGpMb3hIVEFiQmdOVkJBb01GT2FncXVXOGorUzhtdWVrdmtSaGRHRlRhV2R1TVJRd0VnWURWUVFEREF0a1lYUmhjMmxuYmk1cWNEQldNQkFHQnlxR1NNNDlBZ0VHQlN1QkJBQUtBMElBQk0yNW53b21QdlZ1dkdzOGdnZVU2dnUzMmQrK0I3eWJ5MWI1R0JUbkcraFJxd1hnL0xZTFg0RldzQ0htZXFHZzFVZzA1MEhOTHM5WVBqMkdaVEprWVFLamdZWXdnWU13RmdZRFZSMFJCQTh3RFlJTFpHRjBZWE5wWjI0dWFuQXdIUVlEVlIwT0JCWUVGRVVTZUpNOEtTcXg1M0c0ZVUxQjQvMW5qQUhZTUVvR0ExVWRJd1JETUVHaEthUW5NQ1V4SXpBaEJnTlZCQU1NR2xSbGMzUWdRMlZ5ZEdsbWFXTmhkR1VnUVhWMGFHOXlhWFI1Z2hRdHdBL3hzMmxxbzFTRUJXTlhtbWVFaGJqdXF6QU5CZ2txaGtpRzl3MEJBUXNGQUFPQ0FRRUFIZFIzdXV0b0MrUlE3NTBNY0x6OWVGdHpFcnVZa0dVMGFDbkNNenBNSjNITVc2M3BPS0ZWVmhwTnhpcnorcG0vRnBEd0FjTFQxamdLdmRiSDRjYWk4b1RmZDg0R3VFbGR4T3lOWVZySXlia0pPSmxhMXRabG9XNldqR2ZLVlk4WUFhS3dIVlFCY3dhL3N0ZDE4ajNnN0NBL2g5VjR3S1V0UFlMS05vYkFPay9DU0QyQkNIU2R0NDlNUmRrZ3lpZ2p4aDY1NHFrL0RJc3JLejZWVVI3L1VQdnVHdXdQdFpoaElzLzg5T29OWjJ5dk1LQ2ZmTUdITEw5VEtlR0dWVmY5b3pWeFYvbE5ibmVYbUdEMmt2WjF6RmJSd2FZQ213NERjSUFZTGlqMjluYWhib1k4MGhkdDg2SFplNDJlc1FTQkRCemFUeUE0RXZYSDVsNUFVdFNIbWc9PSIsIk1JSUMwVENDQWJrQ0ZDM0FEL0d6YVdxalZJUUZZMWVhWjRTRnVPNnJNQTBHQ1NxR1NJYjNEUUVCQ3dVQU1DVXhJekFoQmdOVkJBTU1HbFJsYzNRZ1EyVnlkR2xtYVdOaGRHVWdRWFYwYUc5eWFYUjVNQjRYRFRJek1URXdPVEF4TkRrME5Wb1hEVEkwTVRFd09EQXhORGswTlZvd0pURWpNQ0VHQTFVRUF3d2FWR1Z6ZENCRFpYSjBhV1pwWTJGMFpTQkJkWFJvYjNKcGRIa3dnZ0VpTUEwR0NTcUdTSWIzRFFFQkFRVUFBNElCRHdBd2dnRUtBb0lCQVFEQ2oyZzFtN1lRWmIxTE1PbE15MnpyT0NnOWNBRXpySzdyY3R5bUZGZDlyNzdKTU9pMWMzbnpJbTZaV2Vtd1NOeEdZMnlVU0IrQ05ISkRRK1c5dk8yTS85RkZ2dUt4TWZWQ0RERUJWMXc5cmtOZGpJR2N2aExBNlZqaHhvQU4wWDRWUm04cHpXN0tLc3I5UE1yMkhaVmJxb3JMVG5Ua0M1YUhob3FWY0xlL09Gbm00TnpVMDJCOXhlY2Fhb3FhalBBWEhsdEZ0RCtEVktFNm1RdVJ0RDhLT0lSaFBmSDlVdW9yT1lWMmVtTEt3MWI3TUZNNU84SUVUY0tEMnRhemNIUlFiRmlvLzZWU1lYaWtCekhJOWJ0dGZkMnFtbVRtVE9MSWhGc2dUYm5acWxHYzltWWIzSEEybVN5blhnMC9OemRPNE1zRi9iaEZtd1N2Q3hCUENZbVJBZ01CQUFFd0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFMekhCZXZPeGJWWEhEOGlIZGhqTHFiVURpWmtPTzg0VU5JRk9JY2VxNEo2ODVxeEphSlo2NVNKVjZmM2d3YlJhM0JDZUpzTEdTa3RjS0o1a2NOOFMzc0ZBQm1Nb1lTQm4vS0xHMjY3Mm9PQ21kYVpWL25vdHVoeDJwd3FyVHROLzVadzE0VWxBNzhiSDhzdUN6SEtwZFNiR1I3clpRbVh5QWtJbDJWVUdPRkNrbFdLeWxzWEZZY29UL2p3Z2dUbGYyNnp3Ynpsa2pJRkZZK3NYcjBuOGdDYWw5bzQwZUhhTldBc2tOZEEyQ3ZpcXg1RklsdG83L09LMGxyS2lKeGtOSThFY0JCUVJlMG1EbnpqNlFYeFlMT3FpaEMxb3dRdWtVY05GSGR5YTRsdlZYMWYxaHgyUkRMWUcycWd2WmVQemY2R3lSSDBtVVM5YXNwNmNCUjhBc2M9Il19.eyJjbmYiOnsia3R5IjoiRUMiLCJjcnYiOiJzZWNwMjU2azEiLCJ4IjoidFZ3VHBCRTB2QVlpdnZXOGlseWtnYWQ4RFM3cHFVcUNiUU9JQ2gwU29nbyIsInkiOiJTalZIbS1QX0NrWUVteURDR2p5OUlWc3Rsb0YyVDB3cUZVREtMeUVER1J3In0sImlhdCI6MCwidHlwZSI6IiIsImlzcyI6IiIsIl9zZCI6WyIySTdTcFVQcE11R1VlMjVDM21wN0ExRG10R1NiQnBnN0Qwb2dRbHI0VWZFIiwicmdyVW42M0ZhblJkTGlqc3pPUEkyN0FOUHA0NnVpV2kxRFdYX3k0VlRqWSIsIndpT2ZDdTUwYmpURldoZkktWGJmNGFlR19WSi0zOWdYV3BXQV9ILVdEUG8iLCIwSFdCUEZmVTFJSjhZYXFsUGdJeEJHQ3QtNlUtTjJJRWw3X1JzMTY5OGdFIiwiOEI1M1BENzdHMkMyajFnd180V0o4a1pTalY3S0I5MjNodVhpMVBWeGpmYyIsIlN2VXdfSFh5cVNqcUxLTHN6dG5GcHlEMkRwb1JsczYybG44SzVMcFUxS0EiLCJfU0lKLUtqTWxzaGtnbkFtYmVxM0pPenduVGhNU0hzZ08wTW1BQXJadHBnIl19.dNzrFlXB98jAUxzMxWG6hgbXb59fsX_Ib0tpYOOh_6d4qsxYIfh8_VkcrcSZmCmVr-p8tjXk_PfEdM25XgP9fA~WyJjRXdQVDU2NmNpSVhMb1VUIiwidmVyaWZpZWRfYXQiLDBd~WyJteE0xRjdOQzBmY1hDdERnIiwibGFzdF9uYW1lIiwiIl0~WyJXYmRLbTVEMzYwZVNNczltIiwiZmlyc3RfbmFtZSIsIiJd~WyJuR05ldXRwTjY4UEFRdGVvIiwiaXNfb3JkZXJfdGhhbl8xNSIsZmFsc2Vd~WyJ0TGljVHJwdWh2c2ZrMmlSIiwiaXNfb3JkZXJfdGhhbl8xOCIsZmFsc2Vd~WyJjYmFBSGY4eUl6RXFack8xIiwiaXNfb3JkZXJfdGhhbl8yMCIsZmFsc2Vd~WyJhUEpQcHhTb2dIMkpKYUhMIiwiaXNfb3JkZXJfdGhhbl82NSIsZmFsc2Vd~"
        val result = SDJwtUtil.getDecodedJwtHeader(sdJwt)

        assertNotNull(result)
        assertEquals("sd+jwt", result!!.getString("typ"))
        assertEquals("ES256K", result.getString("alg"))

        val x5cArray = result.getJSONArray("x5c")
        assertNotNull(x5cArray)
        assertTrue(x5cArray.length() > 0)
    }

    @Test
    fun testDecodeIssuerSignedJwt2() {
        val sdJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImhvZ2UiOiLjgYLjgYTjgYbjgYjjgYrjgYvjgY3jgY_jgZEifQ.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.WCQWdkQo8EkqnBm5VjK4_Wog4f4ItXEhAY1kNbGHb2o~"
        val result = SDJwtUtil.getDecodedJwtHeader(sdJwt)
        assertNotNull(result)
        assertEquals("JWT", result!!.getString("typ"))
        assertEquals("HS256", result.getString("alg"))
    }

    @Test
    fun testGetX509CertificatesFromJwt() {
        val jwt = """
        {
            "typ": "sd+jwt",
            "alg": "ES256K",
            "x5c": [
              "MIIC1jCCAb6gAwIBAgIUWwvaSTr281Wz5GL+B3l+VN5mXZowDQYJKoZIhvcNAQELBQAwJTEjMCEGA1UEAwwaVGVzdCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwHhcNMjMxMTA5MDE1NTMzWhcNMjQxMTA4MDE1NTMzWjBqMQswCQYDVQQGEwJKUDESMBAGA1UECAwJ5p2x5Lqs6YO9MRIwEAYDVQQHDAnmlrDlrr/ljLoxHTAbBgNVBAoMFOagquW8j+S8muekvkRhdGFTaWduMRQwEgYDVQQDDAtkYXRhc2lnbi5qcDBWMBAGByqGSM49AgEGBSuBBAAKA0IABM25nwomPvVuvGs8ggeU6vu32d++B7yby1b5GBTnG+hRqwXg/LYLX4FWsCHmeqGg1Ug050HNLs9YPj2GZTJkYQKjgYYwgYMwFgYDVR0RBA8wDYILZGF0YXNpZ24uanAwHQYDVR0OBBYEFEUSeJM8KSqx53G4eU1B4/1njAHYMEoGA1UdIwRDMEGhKaQnMCUxIzAhBgNVBAMMGlRlc3QgQ2VydGlmaWNhdGUgQXV0aG9yaXR5ghQtwA/xs2lqo1SEBWNXmmeEhbjuqzANBgkqhkiG9w0BAQsFAAOCAQEAHdR3uutoC+RQ750McLz9eFtzEruYkGU0aCnCMzpMJ3HMW63pOKFVVhpNxirz+pm/FpDwAcLT1jgKvdbH4cai8oTfd84GuEldxOyNYVrIybkJOJla1tZloW6WjGfKVY8YAaKwHVQBcwa/std18j3g7CA/h9V4wKUtPYLKNobAOk/CSD2BCHSdt49MRdkgyigjxh654qk/DIsrKz6VUR7/UPvuGuwPtZhhIs/89OoNZ2yvMKCffMGHLL9TKeGGVVf9ozVxV/lNbneXmGD2kvZ1zFbRwaYCmw4DcIAYLij29nahboY80hdt86HZe42esQSBDBzaTyA4EvXH5l5AUtSHmg==",
              "MIIC0TCCAbkCFC3AD/GzaWqjVIQFY1eaZ4SFuO6rMA0GCSqGSIb3DQEBCwUAMCUxIzAhBgNVBAMMGlRlc3QgQ2VydGlmaWNhdGUgQXV0aG9yaXR5MB4XDTIzMTEwOTAxNDk0NVoXDTI0MTEwODAxNDk0NVowJTEjMCEGA1UEAwwaVGVzdCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDCj2g1m7YQZb1LMOlMy2zrOCg9cAEzrK7rctymFFd9r77JMOi1c3nzIm6ZWemwSNxGY2yUSB+CNHJDQ+W9vO2M/9FFvuKxMfVCDDEBV1w9rkNdjIGcvhLA6VjhxoAN0X4VRm8pzW7KKsr9PMr2HZVbqorLTnTkC5aHhoqVcLe/OFnm4NzU02B9xecaaoqajPAXHltFtD+DVKE6mQuRtD8KOIRhPfH9UuorOYV2emLKw1b7MFM5O8IETcKD2tazcHRQbFio/6VSYXikBzHI9bttfd2qmmTmTOLIhFsgTbnZqlGc9mYb3HA2mSynXg0/NzdO4MsF/bhFmwSvCxBPCYmRAgMBAAEwDQYJKoZIhvcNAQELBQADggEBALzHBevOxbVXHD8iHdhjLqbUDiZkOO84UNIFOIceq4J685qxJaJZ65SJV6f3gwbRa3BCeJsLGSktcKJ5kcN8S3sFABmMoYSBn/KLG2672oOCmdaZV/notuhx2pwqrTtN/5Zw14UlA78bH8suCzHKpdSbGR7rZQmXyAkIl2VUGOFCklWKylsXFYcoT/jwggTlf26zwbzlkjIFFY+sXr0n8gCal9o40eHaNWAskNdA2Cviqx5FIlto7/OK0lrKiJxkNI8EcBBQRe0mDnzj6QXxYLOqihC1owQukUcNFHdya4lvVX1f1hx2RDLYG2qgvZePzf6GyRH0mUS9asp6cBR8Asc="
            ]
        }, 
        """.trimIndent()
        val result = SDJwtUtil.getX509CertificatesFromJwt(JSONObject(jwt))

        assertNotNull(result)
        assertEquals(2, result!!.size)

        // 最初の証明書を取得
        val certificate1 = result[0]

        // 基本的なプロパティを検証
        val issuer1 = certificate1.issuerDN.name
        val subject1 = certificate1.subjectDN.name
        assertEquals("CN=Test Certificate Authority", issuer1)
        assertEquals("CN=datasign.jp,O=株式会社DataSign,L=新宿区,ST=東京都,C=JP", subject1)

        val certificate2 = result[1]
        val issuer2 = certificate2.issuerDN.name
        val subject2 = certificate2.subjectDN.name
        assertEquals("CN=Test Certificate Authority", issuer2)
        assertEquals("CN=Test Certificate Authority", subject2)
    }

}