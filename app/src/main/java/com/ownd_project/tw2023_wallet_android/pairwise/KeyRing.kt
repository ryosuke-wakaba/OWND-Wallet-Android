package com.ownd_project.tw2023_wallet_android.pairwise

import org.bitcoinj.core.Address
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicException
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.bitcoinj.wallet.DeterministicSeed
import java.math.BigInteger
import java.security.SecureRandom
import java.util.Base64


data class PublicJwk(
    val kty: String,
    val crv: String,
    val x: String,
    val y: String,
)

data class Jwk(
    val kty: String,
    val crv: String,
    val x: String,
    val y: String,
    val d: String
)

class HDKeyRing(mnemonicWords: String?, entropyLength: Int = 128) {
    private val seed: DeterministicSeed
    private val rootKey: DeterministicKey

    init {
        try {
            seed = if (mnemonicWords == null) {
                // https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki
                DeterministicSeed(SecureRandom(), entropyLength, "")
            } else {
                DeterministicSeed(mnemonicWords.split(" "), null, "", 0)
            }
            // seedが生成できたら、そのままrootKeyを生成
            rootKey = HDKeyDerivation.createMasterPrivateKey(seed.seedBytes)
        } catch (e: MnemonicException) {
            throw IllegalStateException("HDKeyRing instance is unusable due to MnemonicException.", e)
        }
    }

    private fun generateSecondLevelKey(index: Int): DeterministicKey {
        val childNumber = ChildNumber(index, true) // true で hardened
        // path -> m / {index}'
        val child = HDKeyDerivation.deriveChildKey(rootKey, childNumber)
        println("path: ${child.path}, ${child.depth}")
        return child
    }

    fun getAddress(index: Int): String {
        val deterministicKey = generateSecondLevelKey(index)
        val params = MainNetParams.get()

        // DeterministicKey から Bitcoin アドレスを生成
        val address = Address.fromKey(params, deterministicKey, Script.ScriptType.P2PKH)
        return address.toString()
    }

    fun getMnemonicString(): String {
        return seed.mnemonicString!!
    }

    private fun encodeCoordinate(coordinate: ByteArray): String {
        return Base64.getUrlEncoder().encodeToString(coordinate).trimEnd('=')
    }
    private fun extractECKeyComponents(deterministicKey: DeterministicKey): Pair<String, String> {
        val publicKey = deterministicKey.pubKeyPoint
        val xBytes = encodeCoordinate(publicKey.affineXCoord.encoded)
        val yBytes = encodeCoordinate(publicKey.affineYCoord.encoded)
        return Pair(xBytes, yBytes)
    }
    fun getPublicJwk(index: Int): PublicJwk {
        val deterministicKey = generateSecondLevelKey(index)
        val (x, y) = extractECKeyComponents(deterministicKey)
        return PublicJwk(
            kty = "EC",
            crv = "secp256k1",
            x = x,
            y = y
        )
    }

    fun getPrivateJwk(index: Int): Jwk {
        val deterministicKey = generateSecondLevelKey(index)
        val (x, y) = extractECKeyComponents(deterministicKey)
        val d = Base64.getUrlEncoder().encodeToString(deterministicKey.privKeyBytes)
        return Jwk(
            kty = "EC",
            crv = "secp256k1",
            x = x,
            y = y,
            d = d
        )
    }
}
