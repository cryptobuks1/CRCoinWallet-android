/*
 * Created by Eduard Zaydel on 1/4/2019
 * Copyright © 2019 Waves Platform. All rights reserved.
 */

package com.wavesplatform.wallet.v2.data.model.remote.request

import com.google.common.primitives.Bytes
import com.google.common.primitives.Longs
import com.google.gson.annotations.SerializedName
import com.wavesplatform.wallet.v1.crypto.Base58
import com.wavesplatform.wallet.v1.crypto.CryptoProvider
import com.wavesplatform.wallet.v1.ui.auth.EnvironmentManager
import com.wavesplatform.wallet.v1.util.SignUtil
import com.wavesplatform.wallet.v2.data.Constants
import com.wavesplatform.wallet.v2.data.model.local.OrderType
import com.wavesplatform.wallet.v2.data.model.remote.response.OrderBook
import com.wavesplatform.wallet.v2.util.isWaves
import com.wavesplatform.wallet.v2.util.isWavesId
import timber.log.Timber

data class OrderRequest(
        @SerializedName("matcherPublicKey") var matcherPublicKey: String? = "",
        @SerializedName("senderPublicKey") var senderPublicKey: String? = "",
        @SerializedName("assetPair") var assetPair: OrderBook.Pair = OrderBook.Pair(),
        @SerializedName("orderType") var orderType: OrderType = OrderType.BUY,
        @SerializedName("price") var price: Long = 0L,
        @SerializedName("amount") var amount: Long = 0L,
        @SerializedName("timestamp") var timestamp: Long = EnvironmentManager.getTime(),
        @SerializedName("expiration") var expiration: Long = 0L,
        @SerializedName("matcherFee") var matcherFee: Long = 300000,
        @SerializedName("version") var version: Byte = Constants.VERSION,
        @SerializedName("proofs") var proofs: MutableList<String?>? = null,
        @SerializedName("matcherFeeAssetId") var matcherFeeAssetId: String = ""
) {

    private fun toSignByte(): ByteArray {
        return if (matcherFeeAssetId.isWaves() || matcherFeeAssetId.isWavesId()) {
            toSignBytesV2()
        } else {
            toSignBytesV3()
        }
    }

    private fun toSignBytesV2(): ByteArray {
        return try {
            Bytes.concat(
                    byteArrayOf(version),
                    Base58.decode(senderPublicKey),
                    Base58.decode(matcherPublicKey),
                    assetPair.toBytes(),
                    orderType.toBytes(),
                    Longs.toByteArray(price),
                    Longs.toByteArray(amount),
                    Longs.toByteArray(timestamp),
                    Longs.toByteArray(expiration),
                    Longs.toByteArray(matcherFee))
        } catch (e: Exception) {
            Timber.e(e, "Couldn't create toSignBytes")
            ByteArray(0)
        }
    }

    private fun toSignBytesV3(): ByteArray {
        return try {
            version = 3
            Bytes.concat(
                    toSignBytesV2(),
                    SignUtil.arrayOption(matcherFeeAssetId))
        } catch (e: Exception) {
            Timber.e(e, "Couldn't create toSignBytesV3")
            ByteArray(0)
        }
    }

    fun sign(privateKey: ByteArray) {
        proofs = mutableListOf(Base58.encode(CryptoProvider.sign(privateKey, toSignByte())))
    }
}