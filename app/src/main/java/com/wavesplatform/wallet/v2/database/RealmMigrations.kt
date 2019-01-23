package com.wavesplatform.wallet.v2.database

import com.wavesplatform.wallet.v2.util.notNull
import io.realm.DynamicRealm
import io.realm.RealmMigration


class RealmMigrations : RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        if (oldVersion < 1L) {
            val schema = realm.schema.get("Transaction")
            schema.notNull {
                it.addField("script", String::class.java)
                it.addField("minSponsoredAssetFee", String::class.java)
            }
        }
    }
}