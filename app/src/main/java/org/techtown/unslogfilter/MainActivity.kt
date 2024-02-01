package org.techtown.unslogfilter

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.techtown.unslogfilter.databinding.ActivityMainBinding
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val tag: String = javaClass.simpleName

    private val dialog: Dialog by lazy { AppData.showLoadingDialog(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
    }

    private fun initView() = with(binding) {
        val method: String = Thread.currentThread().stackTrace[2].methodName
        AppData.debug(tag, "$method called")

        searchButton.setOnClickListener { getLog() }
        testButton.setOnClickListener { test() }
    }

    private fun ActivityMainBinding.showDialog(show: Boolean = true) {
        if (show) {
            dialog.show()
            searchButton.isEnabled = false
            searchButton.text = "검색 중..."
        } else {
            dialog.hide()
            searchButton.isEnabled = true
            searchButton.text = "검색"
        }
    }

    private fun test() {
        val text = "hello"
        val originalString = "12asdfkaj${text}1212kje12ke"
        val removedString = originalString.replace(Regex("fkaj.*?1212"), "")

        // 12asdkje12ke
        println(removedString)
    }

    private fun ActivityMainBinding.getLog() {
        val method: String = Thread.currentThread().stackTrace[2].methodName
        AppData.debug(tag, "$method called")
        showDialog()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showDialog(false)
            println("모든 파일에 대한 접근 권한 필요")
            return
        }

        val code = codeInput.text.trim().toString()
        if (code.length != 2) {
            showDialog(false)
            AppData.showToast(this@MainActivity, "코드에 2자리 입력 필요")
            return
        }

        val intCode: Int? = code.toIntOrNull()
        if (intCode == null) {
            showDialog(false)
            AppData.showToast(this@MainActivity, "코드에 숫자만 입력 필요")
            return
        }

        if (intCode > 20) {
            showDialog(false)
            AppData.showToast(this@MainActivity, "코드에 20 이하만 입력 필요")
            return
        }

        val folderName = folderInput.text.trim().toString()
        if (folderName.isBlank()) {
            showDialog(false)
            AppData.showToast(this@MainActivity, "폴더명 입력 필요")
            return
        }

        val downloadFolderPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        val filePath = "$downloadFolderPath/server-log/$folderName"
        val folder = File(filePath)
        if (!folder.exists() || !folder.isDirectory) {
            showDialog(false)
            AppData.debug(tag, "폴더를 찾을 수 없음")
            AppData.showToast(this@MainActivity, "폴더를 찾을 수 없음")
            return
        }

        val files = folder.listFiles()
        if (files == null) {
            showDialog(false)
            AppData.debug(tag, "파일을 찾을 수 없음")
            AppData.showToast(this@MainActivity, "파일을 찾을 수 없음")
            return
        }

        val list = mutableListOf<String>()
        lifecycleScope.launch(Dispatchers.IO) {
            for (file in files) {
                val lines = file.readLines()
                for (line in lines) {
                    if (code == "00") {
                        if (line.contains("mns_dispush_004")) {
                            list.add(line.replace(Regex("""\+0900 - debug.*?bswrHedrRmrkVl\":\"\"\},"""), ""))
                        }
                    } else if (line.contains("mns_dispush_004") &&
                        (line.contains(""""oprmCd":"E$code"""") || line.contains(""""oprmCd":"F$code""""))
                    ) {
                        list.add(line.replace(Regex("""\+0900 - debug.*?bswrHedrRmrkVl\":\"\"\},"""), ""))
                    }
                }
            }

            launch(Dispatchers.Main) {
                showDialog(false)
                AppData.debug(tag, "-----------검색완료-----------")
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                val sortedList = list.map { LocalDateTime.parse(it.substring(0, 23), formatter) to it }
                    .sortedBy { it.first }
                    .map { it.second }
                val sb = StringBuilder()
                sortedList.forEach { sb.appendLine(it) }
                AppData.debug(tag, sb.toString())
                AppData.showToast(this@MainActivity, "검색 완료")
            }
        }

    }
}