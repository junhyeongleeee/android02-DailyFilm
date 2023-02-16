package com.boostcamp.dailyfilm.presentation.searchfilm

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.boostcamp.dailyfilm.R
import com.boostcamp.dailyfilm.databinding.ActivitySearchFilmBinding
import com.boostcamp.dailyfilm.presentation.BaseActivity
import com.boostcamp.dailyfilm.presentation.calendar.CalendarActivity
import com.boostcamp.dailyfilm.presentation.calendar.DateFragment
import com.boostcamp.dailyfilm.presentation.playfilm.PlayFilmActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.ArrayList

@AndroidEntryPoint
class SearchFilmActivity : BaseActivity<ActivitySearchFilmBinding>(R.layout.activity_search_film) {

    private val viewModel: SearchFilmViewModel by viewModels()

    private val startForResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            /*if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val calendarIndex = result.data?.getIntExtra(DateFragment.KEY_CALENDAR_INDEX, -1)
                    ?: return@registerForActivityResult
                val dateModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra(PlayFilmFragment.KEY_DATE_MODEL, DateModel::class.java)
                } else {
                    result.data?.getParcelableExtra(PlayFilmFragment.KEY_DATE_MODEL)
                }
                dateModel ?: return@registerForActivityResult
                viewModel.setVideo(calendarIndex, dateModel)
                reloadItem(calendarIndex, dateModel)
            }*/
        }

    override fun initView() {
        binding.viewModel = viewModel
        binding.fragmentManager = supportFragmentManager

        binding.barSearch.setNavigationOnClickListener { finish() }

        observeEvent()
        handleSearchQuery()
    }

    private fun observeEvent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collectLatest { event ->
                    when (event) {
                        is SearchEvent.ItemClickEvent -> {
                            startForResult.launch(
                                Intent(this@SearchFilmActivity, PlayFilmActivity::class.java).apply {
                                    putExtra(
                                        DateFragment.KEY_CALENDAR_INDEX,
                                        0 // TODO: ?? 캘린더에서 접근하는 것이 아닐 땐 무슨 값을 넣어야 하나
                                    )
                                    putExtra(
                                        DateFragment.KEY_DATE_MODEL_INDEX,
                                        event.index
                                    )
                                    putParcelableArrayListExtra(
                                        CalendarActivity.KEY_FILM_ARRAY,
                                        ArrayList(viewModel.itemListFlow.value.map { it?.toDateModel() })
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleSearchQuery() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        (binding.barSearch.menu.findItem(R.id.item_search).actionView as SearchView).apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Log.d("handleIntent", "onQueryTextSubmit: $query")

                    viewModel.searchKeyword(query ?: "")
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    Log.d("handleIntent", "onQueryTextChange: $newText")
                    return false
                }
            })

            setOnCloseListener {
                viewModel.searchKeyword("")
                imm.hideSoftInputFromWindow(windowToken, 0)
                clearFocus()
                false
            }
        }
    }

    companion object {
        const val TAG_DATE_PICKER = "datePicker"
    }
}
