package com.noahseidman.digiid

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.noahseidman.digiid.adapter.MultiTypeDataBoundAdapter
import com.noahseidman.digiid.databinding.FragmentPhraseRestoreBinding
import com.noahseidman.digiid.interfaces.ISetData
import com.noahseidman.digiid.listeners.DialogCompleteCallback
import com.noahseidman.digiid.listeners.PhraseCallback
import com.noahseidman.digiid.listeners.SaveListener
import com.noahseidman.digiid.models.LetterColumnViewModel
import com.noahseidman.digiid.models.MainActivityDataModel
import com.noahseidman.digiid.models.PhraseViewModel
import com.noahseidman.digiid.utils.SeedUtil
import kotlinx.android.synthetic.main.fragment_phrase_restore.*

class PhraseRestore: NotificationFragment(), PhraseCallback, TextWatcher, View.OnClickListener {

    private lateinit var binding: FragmentPhraseRestoreBinding
    private val letters = arrayOf("a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z")
    private lateinit var words: List<String>
    external fun validateRecoveryPhrase(words: Array<String>, phrase: String): Boolean

    @SuppressLint("SetTextI18n")
    override fun onClick(string: String?) {
        if (getSeedLength() < 24) {
            phrase.setText(phrase.text.toString() + " " + string)
        } else {
            processSeed(phrase.text.toString())
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.background -> fadeOutRemove()
            R.id.back -> removeWord()

        }
    }

    companion object {
        fun show(activity: AppCompatActivity, completion: DialogCompleteCallback?) {
            val phraseRestore = PhraseRestore()
            phraseRestore.setCompletion(completion)
            val transaction = activity.supportFragmentManager.beginTransaction()
            transaction.setCustomAnimations(
                R.animator.from_bottom, R.animator.to_bottom,
                R.animator.from_bottom, R.animator.to_bottom
            )
            transaction.add(android.R.id.content, phraseRestore, NotificationFragment::class.java.name)
            transaction.addToBackStack(NotificationFragment::class.java.name)
            transaction.commitAllowingStateLoss()
        }
    }

    override fun autoDismiss(): Boolean {
        return false
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPhraseRestoreBinding.inflate(inflater)
        context?.let {
            words = SeedUtil.getWordList(it)

            val letterColumns = ArrayList<Any>()
            var startIndex = 0
            for (i in 0 until letters.size) {

                val columnWords = ArrayList<Any>()

                for (p in startIndex until words.size) {
                    if (words.get(p).toLowerCase().startsWith(letters.get(i))) {
                        columnWords.add(PhraseViewModel(words.get(p)))
                    } else {
                        startIndex = p + 1
                        if (columnWords.size > 0) {
                            letterColumns.add(LetterColumnViewModel(MultiTypeDataBoundAdapter(this, columnWords)))
                        }
                        break
                    }
                }
            }

            binding.recycler.adapter = MultiTypeDataBoundAdapter(this, letterColumns)
            binding.phrase.addTextChangedListener(this)
            binding.back.setOnClickListener(this)
        }

        background = binding.background
        binding.background.setOnClickListener(this)
        return binding.root;
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.recycler.scrollBy(100, 0)

    }

    override fun afterTextChanged(s: Editable?) {
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    fun getSeedLength(): Int {
        return phrase.text.toString().trim().split(" ").size
    }

    fun removeWord() {
        phrase.setText(phrase.text.toString().trim().split(" ").subList(0, getSeedLength() - 1).joinToString(" "))
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        processSeed(s.toString())
    }

    fun processSeed(s: String) {
        if (validateRecoveryPhrase(words.toTypedArray(), s.trim()) && getSeedLength() == 24) {
            val model = MainActivityDataModel(s.trim())
            val handler = Handler(Looper.getMainLooper())
            context?.let {
                (it as ISetData).setData(model)
                model.save(it, object: SaveListener {
                    override fun onComplete() {
                        handler.post { fadeOutRemove() }
                        handler.postDelayed({
                            (it as MainActivity).restoreSuccessNotification()
                        }, 350)
                    }

                    override fun onFailure() {
                        handler.post { fadeOutRemove() }
                        handler.postDelayed({
                            (it as MainActivity).restoreFailedNotification()
                        }, 350)
                    }
                })
            }
        } else if (getSeedLength() == 24) {
            context?.let {
                Toast.makeText(it, R.string.InvalidSeed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}