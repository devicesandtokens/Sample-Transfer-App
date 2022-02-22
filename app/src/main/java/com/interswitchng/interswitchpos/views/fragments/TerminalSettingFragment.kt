package com.interswitchng.interswitchpos.views.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.interswitchng.interswitchpos.R
import com.interswitchng.interswitchpos.data.repository.AppRepository
import com.interswitchng.interswitchpos.utils.getString
import com.interswitchng.interswitchpos.views.viewmodels.AppViewModel
import com.interswitchng.smartpos.BuildConfig
import com.interswitchng.smartpos.IswPos
import com.interswitchng.smartpos.IswTxnHandler
import com.interswitchng.smartpos.models.core.TerminalInfo
import com.interswitchng.smartpos.shared.Constants
import com.interswitchng.smartpos.shared.interfaces.device.POSDevice
import com.interswitchng.smartpos.shared.utilities.DialogUtils
import com.interswitchng.smartpos.shared.utilities.InputValidator
import com.interswitchng.smartpos.shared.utilities.hide
import com.interswitchng.smartpos.shared.utilities.toast
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.android.synthetic.main.fragment_settings_landing.*
import kotlinx.android.synthetic.main.terminal_settings_page.*
import kotlinx.android.synthetic.main.terminal_settings_page.agentEmail
import kotlinx.android.synthetic.main.terminal_settings_page.agentId
import kotlinx.android.synthetic.main.terminal_settings_page.btnChangePassword
import kotlinx.android.synthetic.main.terminal_settings_page.btnDownloadConfigKimono
import kotlinx.android.synthetic.main.terminal_settings_page.btnDownloadKeys
import kotlinx.android.synthetic.main.terminal_settings_page.btnDownloadTerminalConfig
import kotlinx.android.synthetic.main.terminal_settings_page.configSettings
import kotlinx.android.synthetic.main.terminal_settings_page.etAgentEmail
import kotlinx.android.synthetic.main.terminal_settings_page.etAgentId
import kotlinx.android.synthetic.main.terminal_settings_page.etCallHomeTime
import kotlinx.android.synthetic.main.terminal_settings_page.etCapabilities
import kotlinx.android.synthetic.main.terminal_settings_page.etCountryCode
import kotlinx.android.synthetic.main.terminal_settings_page.etCurrencyCode
import kotlinx.android.synthetic.main.terminal_settings_page.etMerchantCategoryCode
import kotlinx.android.synthetic.main.terminal_settings_page.etMerchantId
import kotlinx.android.synthetic.main.terminal_settings_page.etMerchantNameAndLocation
import kotlinx.android.synthetic.main.terminal_settings_page.etServerIP
import kotlinx.android.synthetic.main.terminal_settings_page.etServerPort
import kotlinx.android.synthetic.main.terminal_settings_page.etServerTimeout
import kotlinx.android.synthetic.main.terminal_settings_page.etServerUrl
import kotlinx.android.synthetic.main.terminal_settings_page.etTerminalId
import kotlinx.android.synthetic.main.terminal_settings_page.progressConfigKimonoDownload
import kotlinx.android.synthetic.main.terminal_settings_page.progressKeyDownload
import kotlinx.android.synthetic.main.terminal_settings_page.progressTerminalDownload
import kotlinx.android.synthetic.main.terminal_settings_page.switchKimono
import kotlinx.android.synthetic.main.terminal_settings_page.switchToNIBBS
import kotlinx.android.synthetic.main.terminal_settings_page.terminalDownloadConfigKimonoContainer
import kotlinx.android.synthetic.main.terminal_settings_page.terminalInfoDownloadContainer
import kotlinx.android.synthetic.main.terminal_settings_page.tvConfigKimono
import kotlinx.android.synthetic.main.terminal_settings_page.tvConfigKimonoDate
import kotlinx.android.synthetic.main.terminal_settings_page.tvIsNibbsTest
import kotlinx.android.synthetic.main.terminal_settings_page.tvKeyDate
import kotlinx.android.synthetic.main.terminal_settings_page.tvKeys
import kotlinx.android.synthetic.main.terminal_settings_page.tvTerminalInfo
import kotlinx.android.synthetic.main.terminal_settings_page.tvTerminalInfoDate
import org.koin.android.ext.android.inject
import org.koin.standalone.KoinComponent
import java.text.SimpleDateFormat
import java.util.*


class TerminalSettingFragment: AppCompatActivity(), KoinComponent{

//    private val store: KeyValueStore by inject()
    private val appViewModel: AppViewModel by inject()
    private val terminalInfo by lazy {
        IswTxnHandler().getTerminalInfo()
    }
    private val isFromSettings by lazy { intent.getBooleanExtra("FROM_SETTINGS", false) }

    private var supervisorCardIsEnrolled = false

    private val alert by lazy {
        DialogUtils.getAlertDialog(this)
                .setTitle("Invalid Configuration")
                .setMessage("The configuration contains invalid parameters, please fix the erros and try saving again")
                .setPositiveButton(android.R.string.ok){ dialog, _ -> dialog.dismiss() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.terminal_settings_page)
        setupButtons()

        setupUi()

        // temporary get key
//        temp(terminalInfo)
    }

//    private fun temp(terminalInfo: TerminalInfo?) {
//        appViewModel.downloadParamsAndKey()
//    }

    private fun setupUi() {
        if (terminalInfo != null) {
            setupTexts(terminalInfo)
        }
    }

    private fun setupButtons(){
        // function validate nibbs request config
        val isValidNibbsRequest: (String, String, String) -> Boolean = { terminalId, ip, port ->

            // check validity of all fields
            val invalidTerminalId = InputValidator(terminalId)
                    .isNotEmpty().isAlphaNumeric()
                    .isExactLength(8)

            val invalidServerIp = InputValidator(ip).isNotEmpty().isValidIp()

            val invalidServerPort = InputValidator(port).isNotEmpty()
                    .isNumber().hasMaxLength(5)
                    .isNumberBetween(0, 65535)


            // show all error on the page
            if (invalidTerminalId.hasError) etTerminalId.error = invalidTerminalId.message
            if (invalidServerIp.hasError) etServerIP.error = invalidServerIp.message
            if (invalidServerPort.hasError) etServerPort.error = invalidServerPort.message


            // ensure no parameter is invalid
            !(invalidTerminalId.hasError
                    && invalidServerIp.hasError
                    && invalidServerPort.hasError)
        }

        btnDownloadConfigKimono.setOnClickListener {
            //disable and hide button
            btnDownloadConfigKimono.isEnabled = false
            btnDownloadConfigKimono.visibility = View.GONE

            //set the text of terminal config
            tvConfigKimono.text = "Downloading Terminal Config"
            //show Progress bar
            progressConfigKimonoDownload.visibility = View.VISIBLE
            //hide download date
            tvConfigKimonoDate.visibility = View.GONE

            //trigger the download of terminal config
            appViewModel.downloadParamsAndKey()
            appViewModel.terminalConfig.observe(this, androidx.lifecycle.Observer {
                println(it)
                it?.let {
                    setupTexts(it)
                    progressConfigKimonoDownload.hide()
                    progressTerminalDownload.hide()
                }
            })

        }

        btnDownloadKeys.setOnClickListener {

            // get fields
            val terminalID: String = etTerminalId.text.toString()
            val serverIp: String = etServerIP.text.toString()
            val serverPort: String = etServerPort.text.toString()

            // check validity
            val isValid = isValidNibbsRequest(terminalID, serverIp, serverPort)

            if (isValid) {
                // disable and hide button
                btnDownloadKeys.isEnabled = false
//                btnDownloadKeys.visibility = View.GONE

                // set the text of keys
                tvKeys.text = getString(com.interswitchng.smartpos.R.string.isw_title_downloading_keys)
                // show progress bar
                progressKeyDownload.visibility = View.VISIBLE
                // hide download date
                tvKeyDate.visibility = View.GONE

                // trigger download keys
                terminalInfo?.let { it1 ->
                    println(switchKimono.isChecked)
                    if(switchKimono.isChecked) {
                        appViewModel.downloadKey(terminalInfo = it1, switchToNIBBS.isChecked)
                    } else {
                        println("got here")
                        appViewModel.downloadNibssKey(terminalInfo = it1, switchToNIBBS.isChecked)
                    }
                    appViewModel.keysDownloadSuccess.observe(this, androidx.lifecycle.Observer {
                        if (it != null) {
                            progressKeyDownload.visibility = View.GONE
                            if (it) {
                                this.toast("Keys download successful", Toast.LENGTH_LONG)
                            } else {
                                this.toast("Keys download not successful", Toast.LENGTH_LONG)
                            }
                        }
                    })

                }
            }
        }

        btnDownloadTerminalConfig.setOnClickListener {

            // get fields
            val terminalID: String = etTerminalId.text.toString()
            val serverIp: String = etServerIP.text.toString()
            val serverPort: String = etServerPort.text.toString()

            // check validity
            val isValid = isValidNibbsRequest(terminalID, serverIp, serverPort)

            // validate terminal id
            if (isValid) {
                // disable and hide button
                btnDownloadTerminalConfig.isEnabled = false
                btnDownloadTerminalConfig.visibility = View.GONE

                // set the text of terminal config
                tvTerminalInfo.text = getString(com.interswitchng.smartpos.R.string.isw_title_downloading_terminal_config)
                // show progress bar
                progressTerminalDownload.visibility = View.VISIBLE
                // hide download date
                tvTerminalInfoDate.visibility = View.GONE

                // trigger download terminal config
                appViewModel.downloadNibss(terminalInfo?.terminalId.toString(), etServerIP.getString(), etServerPort.getString())
                appViewModel.terminalConfig.observe(this, androidx.lifecycle.Observer {
                    println(it)
                    it?.let {
                        setupTexts(it)
                        progressTerminalDownload.hide()
                    }
                })
            }
        }

        switchToEPMS.setOnCheckedChangeListener { button, _ ->
            if(button.isChecked){
                tvIsEPMS.text = "EPMS"
                etServerPort.setText(Constants.ISW_EPMS_PORT)
                etServerIP.setText(Constants.ISW_TERMINAL_IP_EPMS)
            } else{
                tvIsEPMS.text = "CTMS"
                etServerPort.setText(Constants.ISW_CTMS_PORT)
                etServerIP.setText(Constants.ISW_TERMINAL_IP_CTMS)
            }
        }

        switchToNIBBS.setOnCheckedChangeListener { button, _ ->
            if(button.isChecked){
                tvIsNibbsTest.text = "NIBBS TEST"
            } else{
                tvIsNibbsTest.text = "NIBBS PRODUCTION"
            }
        }

        switchKimono.setOnCheckedChangeListener { button, _ ->

            // hide error messages for non required fields
            if (button.isChecked) {

                if (terminalInfo != null) {
                    terminalInfo!!.isKimono = true
                    terminalInfo!!.isKimono3 = true
                }
                // ip and port not required if kimono
                etServerPort.error = null
                etServerIP.error = null

                // hide server and port fields
                etServerPort.isEnabled = false
                etServerIP.isEnabled = false

                etAgentId.isEnabled = true
                etAgentEmail.isEnabled = true

                etMerchantAlias.isEnabled = true
                etMerchantCode.isEnabled = true

                // show server url field
                etServerUrl.isEnabled = true

                configSettings.text = "Configured To Kimono"
            } else {
                // server not required if not kimono
                etServerUrl.error = null
                // hide server url field
                etServerUrl.isEnabled = false

                etAgentId.error = null
                etAgentEmail.error = null

                etAgentId.isEnabled = false
                etAgentEmail.isEnabled = false

                etServerPort.isEnabled = true
                etServerIP.isEnabled = true

                configSettings.text = "Configured to NIBBS"
            }

            terminalInfoDownloadContainer.visibility =
                    if (button.isChecked) View.GONE else View.VISIBLE
            agentId.visibility =
                    if (button.isChecked) View.VISIBLE else View.GONE
            agentEmail.visibility =
                    if (button.isChecked) View.VISIBLE else View.GONE
            iswEpmsContainer.visibility =
                    if (button.isChecked) View.GONE else View.VISIBLE
            iswNibssTestContainer.visibility =
                    if (button.isChecked) View.GONE else View.VISIBLE

            terminalDownloadConfigKimonoContainer.visibility =
                    if (button.isChecked) View.VISIBLE else View.GONE

        }

        btnChangePassword.setOnClickListener {
            if (supervisorCardIsEnrolled) {
                authorizeAndPerformAction {
                    val intent = Intent(this, SetupMerchantCardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                val intent = Intent(this, SetupMerchantCardActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        btnSaveConfig.setOnClickListener {
              saveInfo()
        }
    }

    private fun authorizeAndPerformAction(action: () -> Unit) {
            action.invoke()
    }

    private fun setupTexts(terminalInfo: TerminalInfo? = IswTxnHandler().getTerminalInfo()) {
        val terminalDate = Prefs.getInt(KEY_DATE_TERMINAL, -1)
        val keysDate = Prefs.getInt(KEY_DATE_KEYS, -1)
        val terminalConfigKimono = Prefs.getInt(KEY_DATE_TERMINAL_KIMONO, -1)

        if (terminalDate.toLong() != -1L) {
            val date = Date(terminalDate.toLong())
            val dateStr = SimpleDateFormat.getDateTimeInstance().format(terminalDate)
            tvTerminalInfoDate.text = getString(com.interswitchng.smartpos.R.string.isw_title_date, dateStr)
            tvTerminalInfo.text = getString(com.interswitchng.smartpos.R.string.isw_title_terminal_config_downloaded)
        } else {
            val message = "No terminal configuration"
            tvTerminalInfoDate.text = getString(com.interswitchng.smartpos.R.string.isw_title_date, message)
            tvTerminalInfo.text = getString(com.interswitchng.smartpos.R.string.isw_title_download_terminal_configuration)
        }

        if (keysDate.toLong() != -1L) {
//            val date = Date(terminalDate)
            val dateStr =  SimpleDateFormat.getDateTimeInstance().format(Date())
            tvKeyDate.text = getString(com.interswitchng.smartpos.R.string.isw_title_date, dateStr)
            tvKeys.text = getString(com.interswitchng.smartpos.R.string.isw_title_keys_downloaded)
        } else {
            val message = "No keys"
            tvKeyDate.text = getString(com.interswitchng.smartpos.R.string.isw_title_date, message)
            tvKeys.text = getString(com.interswitchng.smartpos.R.string.isw_title_download_keys)
        }

        if (terminalConfigKimono.toLong() != -1L) {
//            val date = Date(terminalConfigKimono)
            val dateStr =  SimpleDateFormat.getDateTimeInstance().format(Date())
            tvConfigKimonoDate.text = getString(com.interswitchng.smartpos.R.string.isw_title_date, dateStr)
            tvConfigKimono.text = getString(com.interswitchng.smartpos.R.string.isw_title_terminal_config_downloaded)
        } else {
            val message = "No Terminal Configuration"
            tvConfigKimonoDate.text = getString(com.interswitchng.smartpos.R.string.isw_title_date, message)
            tvConfigKimono.text = getString(com.interswitchng.smartpos.R.string.isw_title_download_terminal_configuration)
        }

        // set up field texts with keyTerminalInfo
        terminalInfo?.apply {
            // terminal config
            etTerminalId.setText(terminalId)
            etMerchantId.setText(merchantId)
            etMerchantCategoryCode.setText(merchantCategoryCode)
            etMerchantNameAndLocation.setText(merchantNameAndLocation)
            etCountryCode.setText(countryCode)
            etCurrencyCode.setText(currencyCode)
            etCallHomeTime.setText(callHomeTimeInMin.toString())
            etServerTimeout.setText(serverTimeoutInSec.toString())
            etCapabilities.setText(capabilities)
            etAgentId.setText(agentId)
            etAgentEmail.setText(agentEmail)
            etMerchantAlias.setText(merchantAlias)
            etMerchantCode.setText(merchantCode)

            switchKimono.isChecked = isKimono
//            switchToKimono3.isChecked = isKimono3
        }
        val serverIp = terminalInfo?.serverIp ?: Constants.ISW_TERMINAL_IP
        val serverPort = terminalInfo?.serverPort ?: BuildConfig.ISW_TERMINAL_PORT
        val serverUrl = terminalInfo?.serverUrl ?: Constants.ISW_KIMONO_BASE_URL


        // server config
        etServerIP.setText(serverIp)
        etServerPort.setText(serverPort.toString())
        etServerUrl.setText(serverUrl)
    }

    private fun saveInfo(): TerminalInfo? {
        return terminalInfo?.apply {
            terminalId = etTerminalId.getString()
            merchantId = etMerchantId.getString()
            merchantCategoryCode = etMerchantCategoryCode.getString()
            merchantNameAndLocation = etMerchantNameAndLocation.getString()
            countryCode = etCountryCode.getString()
            currencyCode = etCurrencyCode.getString()
            callHomeTimeInMin = etCallHomeTime.getString().toInt()
            serverTimeoutInSec = etServerTimeout.getString().toInt()
            serverIp = etServerIP.getString()
            serverPort = etServerPort.getString().toInt()
            serverUrl = etServerUrl.getString()
            isKimono = switchKimono.isChecked
            agentId = etAgentId.getString()
            agentEmail = etAgentEmail.getString()
            merchantAlias = etMerchantAlias.getString()
            merchantCode = etMerchantCode.getString()

            // only set capabilities if it was provided
            etCapabilities.getString().apply {
                capabilities = if (this.isNotEmpty()) this else null
            }
            IswTxnHandler().saveTerminalInfo(this)
            this@TerminalSettingFragment.toast("Terminal Config saved", Toast.LENGTH_LONG)
        }

    }


    companion object {
        const val KEY_DATE_TERMINAL = "key_download_terminal_date"
        const val KEY_DATE_KEYS = "key_download_key_date"
        const val KEY_DATE_TERMINAL_KIMONO = "key_download_terminal_date_kimono"
        const val RC_FILE_READ = 49239


        const val AUTHORIZED = 0
        const val FAILED = 1
        const val USE_FINGERPRINT = 2
        const val TAG = "Merchant Card Dialog"
    }
}