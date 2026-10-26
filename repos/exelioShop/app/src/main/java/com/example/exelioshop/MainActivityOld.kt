package com.example.exelioshop

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.exelioshop.bluetooth.BtConnection
import com.example.exelioshop.bluetooth.ReceiveThread
import com.example.exelioshop.bluetooth.SelectDevice
import com.example.exelioshop.classes.Article
import com.example.exelioshop.classes.VMData
import com.example.exelioshop.databinding.ActivityMainOldBinding
import com.example.exelioshop.dataclass.ListItem
import java.io.Serializable

/**
 * =================================================================================================
 * MainActivityOld — Рання експериментальна версія головного POS-екрана exelioShop.
 * -------------------------------------------------------------------------------------------------
 * Призначення:
 * 1. Прототип прямої взаємодії з апаратним РРО через короткі односимвольні команди Datecs (kP, kF, kN, kR).
 * 2. Робота зі сканером штрихкодів через окрему Activity (ScanerActivity на базі ZBar).
 * 3. Редагування та створення товарів у EditItem з подальшою передачею в касу через команду kP.
 * =================================================================================================
 */
class MainActivityOld : AppCompatActivity(), ReceiveThread.Listener, BtConnection.IsConnectListener {

    private val dataModel: VMData by viewModels()
    lateinit var mac: String
    lateinit var binding: ActivityMainOldBinding
    private lateinit var actListLauncher: ActivityResultLauncher<Intent>
    lateinit var btConnection: BtConnection
    private var listItem: ListItem? = null
    var lastCodeChar: Int = 300
    var kpCodeChar: Char = '*'
    var kwCodeChar: Char = '*'
    val articleList: MutableSet<Article.ArticleData> = mutableSetOf()
    lateinit var articleLast: Article
    private var editLauncher: ActivityResultLauncher<Intent>? = null
    private var barCodeLauncher: ActivityResultLauncher<Intent>? = null

    var commandMsg: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainOldBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        onBtListResult()
        init()

        // Лаунчер сканера штрихкодів: отримання результату та передача у фіскальний апарат
        barCodeLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val barCod = it.data?.getSerializableExtra("bar")
                    Toast.makeText(this, "$barCod", Toast.LENGTH_SHORT).show()
                    btConnection.sendMessegeBar(barCod.toString())
                    barCodeLauncher?.launch(
                        Intent(
                            this@MainActivityOld,
                            ScanerActivity::class.java
                        )
                    )
                }
            }

        // Лаунчер створення/редагування товару з формуванням Datecs команди kP (PLU Program)
        editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                var itemToSend: Article.ArticleData =
                    it.data?.getSerializableExtra("articleToSend") as Article.ArticleData

                btConnection.sendMessage("kP\t${itemToSend.code}\t${itemToSend.TAX}\t${itemToSend.section}\t${itemToSend.group}\t${itemToSend.type}\t${itemToSend.price}\t\t${itemToSend.remainder}\t${itemToSend.barCode1}\t${itemToSend.barCode2}\t${itemToSend.barCode3}\t${itemToSend.barCode4}\t${itemToSend.name}\t${itemToSend.productInSet}\t\u0005")
                kwCodeChar = btConnection.com.id.toChar()
            }
        }





        // Відкриття екрану камери для сканування штрихкоду
        binding.buttonScaner.setOnClickListener {
            barCodeLauncher?.launch(Intent(this@MainActivityOld, ScanerActivity::class.java))
            Toast.makeText(this, "ddd", Toast.LENGTH_SHORT).show()
        }

        // Відкриття екрану редактора/створення товару
        binding.newItem.setOnClickListener() {
            editLauncher?.launch(Intent(this@MainActivityOld, EditItem::class.java))
        }

        // Запит бази артикулів: команда kL (PLU List / Last)
        binding.bdList.setOnClickListener() {
            if (lastCodeChar == 300) {
                btConnection.sendMessage("kL\t100000\t")
                lastCodeChar = btConnection.com.id
            } else
                Toast.makeText(this, "$lastCodeChar pppp", Toast.LENGTH_SHORT).show()
        }

        // Тестове надсилання команди читання конкретного артикулу (kR)
        binding.buttonBar.setOnClickListener {
            val kr = "kR\t${binding.artykulNumber.getText().toString()}\t"
            btConnection.sendMessage(kr)
            binding.buttonBar.text = "gg"
        }
    }

    /**
     * Ініціалізація зв'язку через BluetoothAdapter
     */
    private fun init() {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter = btManager.adapter
        btConnection = BtConnection(btAdapter, this, this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.control_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.id_list) {
            actListLauncher.launch(Intent(this, SelectDevice::class.java))
        } else if (item.itemId == R.id.id_connect) {
            listItem.let {
                btConnection.connect(it?.mac!!)
                mac = it.mac
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onBtListResult() {
        actListLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    listItem = it.data?.getSerializableExtra(SelectDevice.DEVICE_KEY) as ListItem
                }
            }
    }

    /**
     * Обробка вхідного потоку відповідей:
     * - Перевірка відповіді на запис (kwCodeChar).
     * - Якщо отримана відповідь на kL (останній артикул) -> запит 1-го артикулу (kF 1).
     * - Якщо отримана відповідь на kF або kN -> збереження артикулу та запит наступного (kN).
     * - Коли артикул збігається з articleLast -> відкриття BDList зі збереженим списком.
     */
    override fun onReceive(message: String) {
        runOnUiThread {
            if (message.length > 4)
                if ((message[0] == ' ' || message[1] == ' ') && message.last() == ' ') {
                    if (kwCodeChar != '*') {
                        binding.textView.text = message
                        kwCodeChar = '*'
                    }
                    commandMsg = message
                    if ((commandMsg[2].toInt() == lastCodeChar && commandMsg[3] == 'k' && commandMsg[4] == 'P') || (commandMsg[3].toInt() == lastCodeChar && commandMsg[4] == 'k' && commandMsg[5] == 'P')) {
                        articleLast = Article(commandMsg)
                        btConnection.sendMessage("kF\t1\t")
                        kpCodeChar = btConnection.com.id.toChar()
                        Toast.makeText(this, "$kpCodeChar", Toast.LENGTH_SHORT).show()
                        binding.textView.text = commandMsg
                        lastCodeChar = 300
                    } else if ((commandMsg[2] == kpCodeChar && commandMsg[3] == 'k' && commandMsg[4] == 'P') || (commandMsg[3] == kpCodeChar && commandMsg[4] == 'k' && commandMsg[5] == 'P'))
                        if (articleLast.article != Article(commandMsg).article) {
                            articleList.add(Article(commandMsg).article)
                            btConnection.sendMessage("kN\t")
                            kpCodeChar = btConnection.com.id.toChar()
                            binding.textView2.text =
                                "${Article(commandMsg).article.code}/${articleLast.article.code}"
                        } else {
                            articleList.add(Article(commandMsg).article)
                            lastCodeChar = 300
                            kpCodeChar = '*'
                            val intentBDList = Intent(this, BDList::class.java)
                            intentBDList.putExtra("item", articleList as Serializable)
                            startActivity(intentBDList)
                        }
                    else
                        binding.textView.text =
                            "${commandMsg[2].toString()},${commandMsg[3].toString()}"
                } else if (kpCodeChar != '*' && message.length > 25) {
                    btConnection.sendMessage("kR\t${articleList.last().code}\t")
                    kpCodeChar = btConnection.com.id.toChar()
                }
        }
    }

    override fun onConnect(boolean: Boolean, mac: String) {
        TODO("Not yet implemented")
    }

    override fun isConn(isConn: Boolean) {
        TODO("Not yet implemented")
    }
}