package com.wavesplatform.wallet.v2.ui.home.quick_action.send.confirmation

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import android.view.animation.AnimationUtils
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.jakewharton.rxbinding2.widget.RxTextView
import com.vicpin.krealmextensions.queryFirst
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v1.request.TransferTransactionRequest
import com.wavesplatform.wallet.v2.data.Constants
import com.wavesplatform.wallet.v2.data.model.remote.response.AssetInfo
import com.wavesplatform.wallet.v2.ui.auth.passcode.enter.EnterPassCodeActivity
import com.wavesplatform.wallet.v2.ui.base.view.BaseActivity
import com.wavesplatform.wallet.v2.ui.home.MainActivity
import com.wavesplatform.wallet.v2.ui.home.quick_action.send.SendPresenter
import com.wavesplatform.wallet.v2.util.launchActivity
import com.wavesplatform.wallet.v2.util.notNull
import com.wavesplatform.wallet.v2.util.showError
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_send_confirmation.*
import pers.victor.ext.click
import pers.victor.ext.gone
import pers.victor.ext.invisiable
import pers.victor.ext.visiable
import pyxis.uzuki.live.richutilskt.utils.runDelayed
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SendConfirmationActivity : BaseActivity(), SendConfirmationView {


    @Inject
    @InjectPresenter
    lateinit var presenter: SendConfirmationPresenter

    @ProvidePresenter
    fun providePresenter(): SendConfirmationPresenter = presenter

    override fun configLayoutRes() = R.layout.activity_send_confirmation

    override fun onCreate(savedInstanceState: Bundle?) {
        translucentStatusBar = true
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    override fun onViewReady(savedInstanceState: Bundle?) {
        setupToolbar(toolbar_view, true,
                getString(R.string.send_confirmation_toolbar_title),
                R.drawable.ic_toolbar_back_white)

        presenter.selectedAsset = intent.extras.getParcelable(KEY_INTENT_SELECTED_ASSET)
        presenter.address = intent.extras.getString(KEY_INTENT_SELECTED_ADDRESS)
        presenter.amount = intent.extras.getString(KEY_INTENT_SELECTED_AMOUNT)

        text_sum.text = "-${presenter.amount}"
        text_tag.text = presenter.selectedAsset!!.getName()
        text_sent_to_address.text = presenter.address
        presenter.getAddressName(presenter.address!!)
        text_fee_value.text = "${SendPresenter.CUSTOM_FEE} ${SendPresenter.CUSTOM_FEE_ASSET_NAME}"

        button_confirm.click { presenter.confirmSend() }

        eventSubscriptions.add(RxTextView.textChanges(edit_optional_message)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    presenter.attachment = it.toString()
                })
    }

    override fun onShowTransactionSuccess(signed: TransferTransactionRequest) {
        toolbar_view.invisiable()
        card_content.gone()
        card_progress.visiable()
        val rotation = AnimationUtils
                .loadAnimation(this@SendConfirmationActivity, R.anim.rotate)
        rotation.fillAfter = true
        image_loader.startAnimation(rotation)
        runDelayed(2000) {
            image_loader.clearAnimation()
            card_progress.gone()
            relative_success.visiable()

            val assetInfo = queryFirst<AssetInfo> {
                equalTo("id", signed.assetId)
            }

            text_leasing_result_value.text = getString(
                    R.string.send_success_you_have_sent_sum,
                    signed.amount.toString(),
                    assetInfo!!.ticker)
            sent_to_address.text = signed.address
            button_okay.click {
                launchActivity<MainActivity>(clear = true)
            }

            add_address.click {

            }
        }
    }

    override fun showAddressBookUser(name: String) {
        if (!TextUtils.isEmpty(name)) {
            text_sent_to_name.text = name
            text_sent_to_name.visiable()
        } else {
            text_sent_to_name.invisiable()
        }
    }

    override fun hideAddressBookUser() {
        text_sent_to_name.invisiable()
    }

    override fun requestPassCode() {
        launchActivity<EnterPassCodeActivity>(
                requestCode = EnterPassCodeActivity.REQUEST_ENTER_PASS_CODE)
    }

    override fun onShowError(res: Int) {
        showError(res, R.id.relative_root)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            EnterPassCodeActivity.REQUEST_ENTER_PASS_CODE -> {
                if (resultCode == Constants.RESULT_OK) {
                    data.notNull { intent ->
                        var passCode = intent.extras
                                .getString(EnterPassCodeActivity.KEY_INTENT_PASS_CODE)
                    }
                } else {
                    setResult(Constants.RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

    companion object {
        const val KEY_INTENT_SELECTED_ASSET = "intent_selected_asset"
        const val KEY_INTENT_SELECTED_ADDRESS = "intent_selected_address"
        const val KEY_INTENT_SELECTED_AMOUNT = "intent_selected_amount"
    }
}
