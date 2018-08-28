package com.wavesplatform.wallet.v2.ui.home.history.details

import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatTextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.vicpin.krealmextensions.queryFirst
import com.wavesplatform.wallet.R
import com.wavesplatform.wallet.v1.crypto.Base58
import com.wavesplatform.wallet.v1.util.MoneyUtil
import com.wavesplatform.wallet.v2.data.Constants
import com.wavesplatform.wallet.v2.data.local.PreferencesHelper
import com.wavesplatform.wallet.v2.ui.base.view.BaseBottomSheetDialogFragment
import com.wavesplatform.wallet.v2.ui.home.profile.address_book.AddressBookActivity
import com.wavesplatform.wallet.v2.ui.home.profile.address_book.AddressBookUser
import com.wavesplatform.wallet.v2.ui.home.profile.address_book.add.AddAddressActivity
import io.github.kbiakov.codeview.CodeView
import io.github.kbiakov.codeview.highlight.ColorThemeData
import io.github.kbiakov.codeview.highlight.SyntaxColors
import kotlinx.android.synthetic.main.fragment_history_bottom_sheet_bottom_btns.view.*
import pyxis.uzuki.live.richutilskt.utils.runDelayed
import kotlin.collections.ArrayList
import pers.victor.ext.*
import com.wavesplatform.wallet.v2.data.model.remote.response.Transaction
import com.wavesplatform.wallet.v2.data.model.remote.response.TransactionType
import com.wavesplatform.wallet.v2.data.model.remote.response.Transfer
import com.wavesplatform.wallet.v2.ui.home.profile.address_book.edit.EditAddressActivity
import com.wavesplatform.wallet.v2.util.*
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


class HistoryDetailsBottomSheetFragment : BaseBottomSheetDialogFragment() {

    var selectedItemPosition: Int = 0
    var selectedItem: Transaction? = null
    var allItems: List<Transaction>? = ArrayList()
    var historyType: String? = ""
    var viewPager: ViewPager? = null
    var rooView: View? = null
    var inflater: LayoutInflater? = null

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        this.inflater = inflater

        rooView = inflater.inflate(R.layout.history_details_bottom_sheet_dialog, container, false)

        setupHistoryViewPager(rooView!!)

        rooView?.findViewById<ImageView>(R.id.image_icon_go_to_preview)?.click {
            viewPager?.currentItem = viewPager?.currentItem!! - 1

            checkStepIconState()
        }
        rooView?.findViewById<ImageView>(R.id.image_icon_go_to_next)?.click {
            viewPager?.currentItem = viewPager?.currentItem!! + 1

            checkStepIconState()
        }

        return rooView
    }

    private fun copyToClipboard(textToCopy: String, view: TextView, btnText: Int) {
        view.text = getString(R.string.common_copied)
        view.setTextColor(findColor(R.color.success400))
        view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_18_success_400, 0, 0, 0);
        runDelayed(1500, {
            this.context.notNull {
                view.text = getString(btnText)
                view.setTextColor(findColor(R.color.black))
                view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_copy_18_black, 0, 0, 0);
            }
        })
    }

    private fun setupView(transaction: Transaction) {
        val historyContainer = rooView?.findViewById<LinearLayout>(R.id.main_container)
        val commentBlock = inflater?.inflate(R.layout.history_detailed_transcation_comment_block_layout, historyContainer, false)
        val baseInfoLayout = inflater?.inflate(R.layout.fragment_history_bottom_sheet_base_info_layout, historyContainer, false)
        val bottomBtns = inflater?.inflate(R.layout.fragment_history_bottom_sheet_bottom_btns, historyContainer, false)

        /**Base info views**/
        val feeValue = baseInfoLayout?.findViewById<TextView>(R.id.text_fee)
        val confirmation = baseInfoLayout?.findViewById<TextView>(R.id.text_confirmations)
        val block = baseInfoLayout?.findViewById<TextView>(R.id.text_block)
        val timeStamp = baseInfoLayout?.findViewById<TextView>(R.id.text_timestamp)
        val status = baseInfoLayout?.findViewById<TextView>(R.id.text_status)

        /**Comment block views**/
        val textComment = commentBlock?.findViewById<TextView>(R.id.text_comment)
        val viewCommentLine = commentBlock?.findViewById<View>(R.id.view_comment_line)

        val showCommentBlock = !transaction.attachment.isNullOrEmpty()
        if (showCommentBlock) {
            viewCommentLine?.gone()
            textComment?.visiable()
            textComment?.text = String(Base58.decode(transaction.attachment))
        } else {
            viewCommentLine?.visiable()
            textComment?.gone()
        }

        if (transaction.feeAssetId.isNullOrEmpty()) {
            feeValue?.text = "${MoneyUtil.getScaledText(transaction.fee, 8).stripZeros()} ${Constants.defaultAssets[0].issueTransaction?.name}"
        } else {
            // TODO: Need call details of asset
        }
        confirmation?.text = (preferencesHelper.currentBlocksHeight - transaction.height).toString()
        block?.text = transaction.height.toString()
        timeStamp?.text = transaction.timestamp.date("dd.MM.yyyy 'at' HH:mm")

        if (preferencesHelper.currentBlocksHeight.minus(transaction.height) > 0) {
            status?.setBackgroundResource(R.drawable.success400_01_shape)
            status?.text = getString(R.string.history_details_completed)
            status?.setTextColor(findColor(R.color.success500))
        } else {
            status?.setBackgroundResource(R.drawable.warning400_01_shape)
            status?.text = getString(R.string.history_details_unconfirmed)
            status?.setTextColor(findColor(R.color.warning600))
        }


        /** Add click to copy buttons **/
        bottomBtns?.container_copy_tx_id?.click {
            copyToClipboard(transaction.id, bottomBtns.text_copy_tx_id, R.string.history_details_copy_tx_id)
        }
        bottomBtns?.container_copy_all_data?.click {
            copyToClipboard(transaction.id, bottomBtns.text_copy_all_data, R.string.history_details_copy_all_data)
        }

        historyContainer?.removeAllViews()

        when (transaction.transactionType()) {
            TransactionType.RECEIVED_TYPE, TransactionType.MASS_RECEIVE_TYPE, TransactionType.SPAM_RECEIVE_TYPE, TransactionType.MASS_SPAM_RECEIVE_TYPE, TransactionType.UNRECOGNISED_TYPE -> {
                val receiveView = inflater?.inflate(R.layout.fragment_bottom_sheet_receive_layout, historyContainer, false)
                val receivedFromName = receiveView?.findViewById<AppCompatTextView>(R.id.text_received_from_name)
                val receivedFromAddress = receiveView?.findViewById<AppCompatTextView>(R.id.text_received_from_address)
                val imageAddressAction = receiveView?.findViewById<AppCompatImageView>(R.id.image_address_action)

                receivedFromAddress?.text = transaction.sender

                resolveExistOrNoAddress(receivedFromName, receivedFromAddress, imageAddressAction)

                historyContainer?.addView(receiveView)
                historyContainer?.addView(commentBlock)
                historyContainer?.addView(baseInfoLayout)
            }
            TransactionType.SENT_TYPE -> {
                val sendView = inflater?.inflate(R.layout.fragment_bottom_sheet_send_layout, historyContainer, false)
                val resendBtn = inflater?.inflate(R.layout.resen_btn, historyContainer, false)
                val sentToName = sendView?.findViewById<AppCompatTextView>(R.id.text_sent_to_name)
                val sentAddress = sendView?.findViewById<AppCompatTextView>(R.id.text_sent_address)
                val imageAddressAction = sendView?.findViewById<AppCompatImageView>(R.id.image_address_action)

                sentAddress?.text = transaction.recipient

                resolveExistOrNoAddress(sentToName, sentAddress, imageAddressAction)

                historyContainer?.addView(sendView)
                historyContainer?.addView(commentBlock)
                historyContainer?.addView(baseInfoLayout)
                historyContainer?.addView(resendBtn)
            }
            TransactionType.STARTED_LEASING_TYPE -> {
                val startLeaseView = inflater?.inflate(R.layout.fragment_bottom_sheet_start_lease_layout, historyContainer, false)
                val cancelLeasingBtn = inflater?.inflate(R.layout.cancel_leasing_btn, historyContainer, false)
                val textLeasingToName = startLeaseView?.findViewById<TextView>(R.id.text_leasing_to_name)
                val textLeasingToAddress = startLeaseView?.findViewById<AppCompatTextView>(R.id.text_leasing_to_address)
                val imageAddressAction = startLeaseView?.findViewById<AppCompatImageView>(R.id.image_address_action)

                textLeasingToAddress?.text = transaction.lease?.recipient

                resolveExistOrNoAddress(textLeasingToName, textLeasingToAddress, imageAddressAction)

                historyContainer?.addView(startLeaseView)
                historyContainer?.addView(commentBlock)
                historyContainer?.addView(baseInfoLayout)
                historyContainer?.addView(cancelLeasingBtn)
            }
            TransactionType.EXCHANGE_TYPE -> {
                val exchangeView = inflater?.inflate(R.layout.fragment_bottom_sheet_exchange_layout, historyContainer, false)
                val btcPrice = exchangeView?.findViewById<TextView>(R.id.text_btc_price)

                historyContainer?.addView(exchangeView)
                historyContainer?.addView(commentBlock)
                historyContainer?.addView(baseInfoLayout)
            }
            TransactionType.SELF_TRANSFER_TYPE -> {
//                val selfTransfer = inflater?.inflate(R.layout.fragment_bottom_sheet_self_trans_layout, historyContainer, false)
                viewCommentLine?.gone()

//                historyContainer?.addView(selfTransfer)
                if (showCommentBlock) {
                    historyContainer?.addView(commentBlock)
                } else {
                    baseInfoLayout?.setMargins(top = dp2px(16))
                }
                historyContainer?.addView(baseInfoLayout)
            }
            TransactionType.CANCELED_LEASING_TYPE, TransactionType.INCOMING_LEASING_TYPE -> {
                val receiveView = inflater?.inflate(R.layout.fragment_bottom_sheet_cancel_or_incoming_leasing_layout, historyContainer, false)

                val textCancelLeasingFromName = receiveView?.findViewById<AppCompatTextView>(R.id.text_cancel_leasing_from_name)
                val textCancelLeasingFromAddress = receiveView?.findViewById<AppCompatTextView>(R.id.text_cancel_leasing_from_address)
                val imageAddressAction = receiveView?.findViewById<AppCompatImageView>(R.id.image_address_action)


                textCancelLeasingFromAddress?.text = transaction.sender

                resolveExistOrNoAddress(textCancelLeasingFromName, textCancelLeasingFromAddress, imageAddressAction)

                historyContainer?.addView(receiveView)
                historyContainer?.addView(commentBlock)
                historyContainer?.addView(baseInfoLayout)
            }
            TransactionType.TOKEN_GENERATION_TYPE, TransactionType.TOKEN_BURN_TYPE, TransactionType.TOKEN_REISSUE_TYPE -> {
                val tokenView = inflater?.inflate(R.layout.fragment_bottom_sheet_token_layout, historyContainer, false)
                val textIdValue = tokenView?.findViewById<TextView>(R.id.text_id_value)
                val textTokenStatus = tokenView?.findViewById<TextView>(R.id.text_token_status)

                textIdValue?.text = transaction.assetId
                if (transaction.reissuable) {
                    textTokenStatus?.text = getString(R.string.history_details_reissuable)
                } else {
                    textTokenStatus?.text = getString(R.string.history_details_not_reissuable)
                }

                historyContainer?.addView(tokenView)
                historyContainer?.addView(commentBlock)
                historyContainer?.addView(baseInfoLayout)
            }
            TransactionType.CREATE_ALIAS_TYPE -> {
                if (showCommentBlock) {
                    historyContainer?.addView(commentBlock)
                } else {
                    baseInfoLayout?.setMargins(top = dp2px(16))
                }
                historyContainer?.addView(baseInfoLayout)
            }
            TransactionType.MASS_SEND_TYPE -> {
                val massSendLayout = inflater?.inflate(R.layout.fragment_bottom_sheet_mass_send_layout, null, false)
                val addressContainer = massSendLayout?.findViewById<LinearLayout>(R.id.container_address)
                val showMoreAddress = massSendLayout?.findViewById<TextView>(R.id.text_show_more_address)
                val button = inflater?.inflate(R.layout.resen_btn, null, false)

                val transfers: MutableList<Transfer> = transaction.transfers.toMutableList()

                transfers.forEachIndexed { index, transfer ->
                    val addressView = inflater?.inflate(R.layout.address_layout, null, false)
                    val textSentAddress = addressView?.findViewById<TextView>(R.id.text_sent_address)
                    val textSentAmount = addressView?.findViewById<TextView>(R.id.text_sent_amount)
                    val imageAddressAction = addressView?.findViewById<AppCompatImageView>(R.id.image_address_action)
                    val viewDivider = addressView?.findViewById<AppCompatImageView>(R.id.view_divider)

                    resolveExistOrNoAddressForMassSend(textSentAddress, imageAddressAction)

                    textSentAddress?.text = transfer.recipient
                    textSentAmount?.text = MoneyUtil.getScaledText(transfer.amount, transaction.asset)

                    if (index >= 3) {
                        showMoreAddress?.visiable()
                        showMoreAddress?.text = getString(R.string.history_details_show_all, (transfers.size).toString())

                        showMoreAddress?.click {
                            showMoreAddress.gone()

                            for (i in 3 until transfers.size) {
                                val addressView = inflater?.inflate(R.layout.address_layout, null, false)

                                val textSentAddress = addressView?.findViewById<AppCompatTextView>(R.id.text_sent_address)
                                val textSentAmount = addressView?.findViewById<AppCompatTextView>(R.id.text_sent_amount)
                                val imageAddressAction = addressView?.findViewById<AppCompatImageView>(R.id.image_address_action)
                                val viewDivider = addressView?.findViewById<AppCompatImageView>(R.id.view_divider)

                                val transfer = transfers[i]

                                resolveExistOrNoAddressForMassSend(textSentAddress, imageAddressAction)

                                textSentAddress?.text = transfer.recipient
                                textSentAmount?.text = MoneyUtil.getScaledText(transfer.amount, transaction.asset)

                                if (i == transfers.size - 1) viewDivider?.gone()

                                addressContainer?.addView(addressView)
                            }
                        }
                    } else {
                        if (index == transfers.size - 1) viewDivider?.gone()

                        addressContainer?.addView(addressView)
                    }
                }

                historyContainer?.addView(massSendLayout)
                historyContainer?.addView(commentBlock)
                historyContainer?.addView(baseInfoLayout)
                historyContainer?.addView(button)
            }
            TransactionType.DATA_TYPE -> {
                val dataView = inflater?.inflate(R.layout.fragment_bottom_sheet_data_layout, historyContainer, false)
                val codeView = dataView?.findViewById<CodeView>(R.id.code_view)
                val imageCopyData = dataView?.findViewById<AppCompatImageView>(R.id.image_copy_data)

                val customTheme = ColorThemeData(SyntaxColors(android.R.color.transparent, R.color.submit300, android.R.color.transparent, android.R.color.transparent, R.color.basic700,
                        android.R.color.transparent, R.color.basic700, android.R.color.transparent, android.R.color.transparent, android.R.color.transparent, android.R.color.transparent),
                        R.color.basic50, android.R.color.transparent, android.R.color.transparent, R.color.basic50)

                val code = "{\n" +
                        "\t\"key\" : \"test long\",\n" +
                        "\t\"type\" : \"integer\",\n" +
                        "\t\"value\" : 1001\n" +
                        "}, {\n" +
                        "\t\"key\" : \"test true\",\n" +
                        "\t\"type\" : \"boolean\",\n" +
                        "\t\"value\" : true\n" +
                        "}, {\n" +
                        "\t\"key\" : \"test false\",\n" +
                        "\t\"type\" : \"boolean\",\n" +
                        "\"value\" : true\n" +
                        "}"
                codeView?.setCode(code);
                codeView?.getOptions()?.withTheme(customTheme)
                imageCopyData?.click {
                    it.copyToClipboard(code)
                }
                historyContainer?.addView(dataView)
                historyContainer?.addView(baseInfoLayout)
            }
        }
        historyContainer?.addView(bottomBtns)
    }

    private fun resolveExistOrNoAddress(textViewName: TextView?, textViewAddress: TextView?, imageAddressAction: AppCompatImageView?) {
        val addressBookUser = queryFirst<AddressBookUser> { equalTo("address", textViewAddress?.text.toString()) }
        if (addressBookUser == null) {
            //  not exist
            textViewName?.gone()
            textViewAddress?.textSize = 14f
            imageAddressAction?.setImageResource(R.drawable.ic_add_address_submit_300)

            imageAddressAction?.click {
                launchActivity<AddAddressActivity>(AddressBookActivity.REQUEST_ADD_ADDRESS) {
                    putExtra(AddressBookActivity.BUNDLE_TYPE, AddressBookActivity.SCREEN_TYPE_NOT_EDITABLE)
                    putExtra(AddressBookActivity.BUNDLE_ADDRESS_ITEM, AddressBookUser(textViewAddress?.text.toString(), ""))
                }
            }
        } else {
            // exist
            textViewName?.text = addressBookUser.name
            textViewName?.visiable()
            textViewAddress?.textSize = 12f
            imageAddressAction?.setImageResource(R.drawable.ic_edit_address_submit_300)


            imageAddressAction?.click {
                launchActivity<EditAddressActivity>(AddressBookActivity.REQUEST_EDIT_ADDRESS) {
                    putExtra(AddressBookActivity.BUNDLE_TYPE, AddressBookActivity.SCREEN_TYPE_NOT_EDITABLE)
                    putExtra(AddressBookActivity.BUNDLE_ADDRESS_ITEM, AddressBookUser(textViewAddress?.text.toString(), addressBookUser.name))
                }
            }
        }
    }

    private fun resolveExistOrNoAddressForMassSend(textViewAddress: TextView?, imageAddressAction: AppCompatImageView?) {
        val addressBookUser = queryFirst<AddressBookUser> { equalTo("address", textViewAddress?.text.toString()) }
        if (addressBookUser == null) {
            //  not exist
            imageAddressAction?.setImageResource(R.drawable.ic_add_address_submit_300)

            imageAddressAction?.click {
                launchActivity<AddAddressActivity>(AddressBookActivity.REQUEST_ADD_ADDRESS) {
                    putExtra(AddressBookActivity.BUNDLE_TYPE, AddressBookActivity.SCREEN_TYPE_NOT_EDITABLE)
                    putExtra(AddressBookActivity.BUNDLE_ADDRESS_ITEM, AddressBookUser(textViewAddress?.text.toString(), ""))
                }
            }
        } else {
            // exist
            textViewAddress?.text = addressBookUser.name

            imageAddressAction?.setImageResource(R.drawable.ic_edit_address_submit_300)

            imageAddressAction?.click {
                launchActivity<EditAddressActivity>(AddressBookActivity.REQUEST_EDIT_ADDRESS) {
                    putExtra(AddressBookActivity.BUNDLE_TYPE, AddressBookActivity.SCREEN_TYPE_NOT_EDITABLE)
                    putExtra(AddressBookActivity.BUNDLE_ADDRESS_ITEM, AddressBookUser(textViewAddress?.text.toString(), addressBookUser.name))
                }
            }
        }
    }

    private fun setupHistoryViewPager(view: View) {
        viewPager = view.findViewById(R.id.viewpager_history_item)
        val historyDetailsAdapter = HistoryDetailsAdapter(activity!!, allItems!!, historyType)
        viewPager?.adapter = historyDetailsAdapter
        viewPager?.currentItem = selectedItemPosition

        val enumList = HistoryTypeEnum.values()

        viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                val historyItem = historyDetailsAdapter.mData[position]
                selectedItem = historyItem
                selectedItemPosition = position

                setupView(historyItem)

                checkStepIconState()
            }
        })

        checkStepIconState()

        selectedItem?.let {
            setupView(it)
        }
    }

    private fun checkStepIconState() {
        if (viewPager?.currentItem == 0 && viewPager?.adapter?.count == 1) {
            rooView?.findViewById<ImageView>(R.id.image_icon_go_to_preview)?.alpha = 0.5F
            rooView?.findViewById<ImageView>(R.id.image_icon_go_to_next)?.alpha = 0.5F
        } else if (viewPager?.currentItem == 0) {
            rooView?.findViewById<ImageView>(R.id.image_icon_go_to_preview)?.alpha = 0.5F
            rooView?.findViewById<ImageView>(R.id.image_icon_go_to_next)?.alpha = 1.0F
        } else if (viewPager?.currentItem == viewPager?.adapter?.count!! - 1) {
            rooView?.findViewById<ImageView>(R.id.image_icon_go_to_preview)?.alpha = 1.0F
            rooView?.findViewById<ImageView>(R.id.image_icon_go_to_next)?.alpha = 0.5F
        } else {
            rooView?.findViewById<ImageView>(R.id.image_icon_go_to_preview)?.alpha = 1.0F
            rooView?.findViewById<ImageView>(R.id.image_icon_go_to_next)?.alpha = 1.0F
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AddressBookActivity.REQUEST_EDIT_ADDRESS || requestCode == AddressBookActivity.REQUEST_ADD_ADDRESS){
            if (resultCode == Constants.RESULT_OK){
                selectedItem?.let {
                    setupView(it)
                }
            }
        }
    }
}