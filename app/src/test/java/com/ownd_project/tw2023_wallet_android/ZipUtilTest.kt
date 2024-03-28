package com.ownd_project.tw2023_wallet_android

import com.ownd_project.tw2023_wallet_android.utils.ZipUtil
import org.junit.Assert
import org.junit.Test

class ZipUtilTest {
    @Test
    fun testCompressAndDecompress() {
        val originalString = "これはテストの文字列です。"

        // 文字列を圧縮
        val compressed = ZipUtil.compressString(originalString)

        // 圧縮した文字列を解凍
        val decompressed = ZipUtil.decompressString(compressed)
        val scanned = "H4sIAAAAAAAAAKVTy46jRhT9F9bdEYa208yONgYXQ+Ex5mEqjiwe7qHKVYAa2xhG/QkjZZf8QDTb2ed38viNXOyk00kUKVEWV1CPe+4995z6ID1WTyI5SG8k1h62p2zLmqqUbqTsaZfvygNNOBztOvspiZZ0Qe0qn3vtgt6f8mhc5yLsMoXLu9WY7yzzkFln7ihuRyKPp6VXx9G5d3qs4h5RZ2rfuevgTxjY1494etc5DE0Whn6Hp5pM1q7sRC7PIJ8IzuK1xxFtKWCViFXUC3Hn+vGAJ+/W+oD3Nhwt6ePyC2izzlT8UiLrq5OjhsdkTQDOrfP5/kjWRZGuHxqy0uCuOyJrG1oMFNfX5cUKNah0RzFFEySgvFG/0FqIkBIIHMUq9m0eC6wQZjLMMoX4M6CMO3zJJwyx+ku0f2CpMgY6hQzrFIlCzucP/WV0aq46ZdY7QutIpx2zTmOZCPckGstJZDawVjAFrP+W89LrNd/OYUQ1mQ5cBhx94nTaKBVcGTgna2/siPAujkZtagWnWLU5scJjbvEineMT7rP+0kPpjTMLZOMNDUHuJCIgq92Q4LXM7SCH+Xt9zwpL6I0vX2NOr/1cZ47+p0akSC2+90r7lK6AH0MtWOmAjRmEGWBDn1zjIblY53UfJugrah6rYOeyob/Zeg/fyUtdH84F4TEzhdsP2trMFfE57pHqiuCMRQi6zwbOR5gfH2wZKFfuaBYqgyZoZk6X3aChV+TD/CjqsIFkx9fPjo/k0NDbBbtGRG3tcdVCLyZ4pTq7vQ4WxzI2cAu2Nio8r9gysCp6NsP7tM3Z1ECp1ghl+XZh3eqFqHPrHt3WbuH7vGq0+mRP4qwoq3cdr0n5/mRO0220TFa5dhAWnbtb/B7eeE6bmicdPPAPG6lMxG4jvfnqj9+N5F5+bjYSr7KEX/d25W2w2kjPN68v/vTNx18+ff7rVZZs7Xdw9WvYzpPD3+CNy96/gP/52+9//OG7f4R/lp5/BRca96TIBAAA"
        val scanned2 = "H4sIAAAAAAAAAKVTy46jRhT9F9bdEYa208yONgYXQ+Ex5mEqjiwe7qHKVYAa2xhG/QkjZZf8QDTb2ed38viNXOyk00kUKVEWV1CPe+4995z6ID1WTyI5SG8k1h62p2zLmqqUbqTsaZfvygNNOBztOvspiZZ0Qe0qn3vtgt6f8mhc5yLsMoXLu9WY7yzzkFln7ihuRyKPp6VXx9G5d3qs4h5RZ2rfuevgTxjY1494etc5DE0Whn6Hp5pM1q7sRC7PIJ8IzuK1xxFtKWCViFXUC3Hn+vGAJ+/W+oD3Nhwt6ePyC2izzlT8UiLrq5OjhsdkTQDOrfP5/kjWRZGuHxqy0uCuOyJrG1oMFNfX5cUKNah0RzFFEySgvFG/0FqIkBIIHMUq9m0eC6wQZjLMMoX4M6CMO3zJJwyx+ku0f2CpMgY6hQzrFIlCzucP/WV0aq46ZdY7QutIpx2zTmOZCPckGstJZDawVjAFrP+W89LrNd/OYUQ1mQ5cBhx94nTaKBVcGTgna2/siPAujkZtagWnWLU5scJjbvEineMT7rP+0kPpjTMLZOMNDUHuJCIgq92Q4LXM7SCH+Xt9zwpL6I0vX2NOr/1cZ47+p0akSC2+90r7lK6AH0MtWOmAjRmEGWBDn1zjIblY53UfJugrah6rYOeyob/Zeg/fyUtdH84F4TEzhdsP2trMFfE57pHqiuCMRQi6zwbOR5gfH2wZKFfuaBYqgyZoZk6X3aChV+TD/CjqsIFkx9fPjo/k0NDbBbtGRG3tcdVCLyZ4pTq7vQ4WxzI2cAu2Nio8r9gysCp6NsP7tM3Z1ECp1ghl+XZh3eqFqHPrHt3WbuH7vGq0+mRP4qwoq3cdr0n5/mRO0220TFa5dhAWnbtb/B7eeE6bmicdPPAPG6lMxG4jvfnqj9+N5F5+bjYSr7KEX/d25W2w2kjPN68v/vTNx18+ff7rVZZs7Xdw9WvYzpPD3+CNy96/gP/52+9//OG7f4R/lp5/BRca96TIBAAA"
        val decompressed2 = ZipUtil.decompressString(scanned2)
        println(decompressed2)

        // 元の文字列と解凍後の文字列が同じであることを確認
        Assert.assertEquals(originalString, decompressed)
    }
}