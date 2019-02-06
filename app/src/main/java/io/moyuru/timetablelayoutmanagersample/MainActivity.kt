package io.moyuru.timetablelayoutmanagersample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.jakewharton.threetenabp.AndroidThreeTen
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import io.moyuru.timetablelayout.TimetableLayoutManager

class MainActivity : AppCompatActivity() {

  private val binding by lazy { DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AndroidThreeTen.init(this)

    val adapter = GroupAdapter<ViewHolder>()
    val periods = fillWithSpacer(createPrograms())
    binding.recyclerView.addItemDecoration(Decoration(periods, 130.dp, 2.dp, 64.dp, 64.dp))
    binding.recyclerView.layoutManager = TimetableLayoutManager(130.dp, 2.dp) {
      val period = periods[it]
      TimetableLayoutManager.PeriodInfo(period.startAt, period.endAt, period.stageNumber)
    }
    binding.recyclerView.adapter = adapter
    periods.map {
      when (it) {
        is EmptyPeriod -> SpaceItem()
        is Program -> ProgramItem(it)
      }
    }.let(adapter::update)
  }

  private fun fillWithSpacer(programs: List<Program>): List<Period> {
    if (programs.isEmpty()) return programs

    val sortedPrograms = programs.sortedBy { it.startAt }
    val firstProgramStartAt = sortedPrograms.first().startAt
    val lastProgramEndAt = sortedPrograms.maxBy { it.endAt }?.endAt ?: return programs
    val stageNumbers = sortedPrograms.map { it.stageNumber }.distinct()

    val filledPeriod = ArrayList<Period>()
    stageNumbers.forEach { roomNumber ->
      val sessionsInSameRoom = sortedPrograms.filter { it.stageNumber == roomNumber }
      sessionsInSameRoom.forEachIndexed { index, session ->
        if (index == 0 && session.startAt > firstProgramStartAt)
          filledPeriod.add(EmptyPeriod(firstProgramStartAt, session.startAt, roomNumber))

        filledPeriod.add(session)

        if (index == sessionsInSameRoom.size - 1 && session.endAt < lastProgramEndAt) {
          filledPeriod.add(EmptyPeriod(session.endAt, lastProgramEndAt, roomNumber))
        }

        val nextSession = sessionsInSameRoom.getOrNull(index + 1) ?: return@forEachIndexed
        if (session.endAt != nextSession.startAt)
          filledPeriod.add(EmptyPeriod(session.endAt, nextSession.startAt, roomNumber))
      }
    }
    return filledPeriod.sortedBy { it.startAt }
  }
}
