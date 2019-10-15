@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package frc.robot.subsystems.intake

import edu.wpi.first.wpilibj2.command.CommandBase
import edu.wpi.first.wpilibj2.command.InstantCommand
import frc.robot.Controls
import frc.robot.Controls.driverFalconXbox
import frc.robot.subsystems.superstructure.Superstructure
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ghrobotics.lib.commands.FalconCommand
import org.ghrobotics.lib.commands.sequential
import org.ghrobotics.lib.mathematics.units.derived.volt
import org.ghrobotics.lib.wrappers.hid.kA
import kotlin.math.abs

val closeIntake = InstantCommand(Runnable { Intake.wantsOpen = false })
val openIntake = InstantCommand(Runnable { Intake.wantsOpen = true })

val aButton = 1

class HatchStateMachineCommand() : FalconCommand() {
//    val possibleHatchStates = arrayOf(Superstructure.kHatchLow, Superstructure.kHatchMid, Superstructure.kHatchHigh)
//        @Synchronized get

    val possibleHatchStates = hashMapOf(0 to Superstructure.kHatchLow, 1 to Superstructure.kHatchMid, 2 to Superstructure.kHatchHigh)
        @Synchronized get

    var currentCommand: CommandBase = CommandBase sequential {  }
    var wasInitilized = false


    var wasPressed = false
    val source by lazy { driverFalconXbox.getRawButton(aButton) }

    // 0 = lv1, 1=lv2, 2=lv3
    var currentHatchState = 0
    override fun initialize() {
        println("State machine activated")
    }
    override fun execute() {
        if (source() && !wasPressed) {
            // button is pressed
//            currentHatchState = (currentHatchState + 1) % (possibleHatchStates.size - 1) // increment hatch state
            currentHatchState = if(currentHatchState == (possibleHatchStates.size - 1)) 0 else currentHatchState + 1
            println("currentHatchState: ${currentHatchState}")
//            println("possibleHatchStates.size: ${possibleHatchStates.size}")
            currentCommand.end(true)
            currentCommand = when (currentHatchState) {
                0 -> Superstructure.kHatchLow
                1 -> Superstructure.kHatchMid
                2 -> Superstructure.kHatchHigh
                else -> sequential {  }
            }

            currentCommand.schedule()


            wasPressed = true
        } else if(!source()) {
            wasPressed = false
        }

//        if(currentCommand.isFinished) currentCommand = sequential {}

//        try {
//            currentCommand.execute()
//        } catch(E: Exception) { E.printStackTrace()}
    }
}

class IntakeHatchCommand(val releasing: Boolean) : FalconCommand(Intake) {

    override fun initialize() {
        println("intaking hatch command")
        Intake.hatchMotorOutput = 12.volt * (if (releasing) -1 else 1)
        Intake.cargoMotorOutput = 0.volt
        Intake.wantsOpen = false
    }

    override fun end(interrupted: Boolean) {
        Intake.wantsOpen = false
        Intake.hatchMotorOutput = 0.volt
        Intake.cargoMotorOutput = 0.volt
    }
}

class IntakeCargoCommand(val releasing: Boolean) : FalconCommand(Intake) {

//    var wasOpen: Boolean = false

    override fun initialize() {
        println("${if (releasing) "releasing" else "intaking"} cargo command!")
//        wasOpen = Intake.wantsOpen
        Intake.wantsOpen = !releasing

        Intake.hatchMotorOutput = 12.volt * (if (releasing) 1 else -1)
        Intake.cargoMotorOutput = 12.volt * (if (!releasing) 1 else -1)

        super.initialize()
    }

    override fun end(interrupted: Boolean) {
        Intake.wantsOpen = false
        Intake.cargoMotorOutput = 3.volt
        Intake.hatchMotorOutput = 3.volt
        GlobalScope.launch {
            delay(500)
            Intake.cargoMotorOutput = 0.volt
            Intake.hatchMotorOutput = 0.volt
        }
        super.end(interrupted)
    }
}

/*
class IntakeTeleopCommand : FalconCommand(Intake) {


    override fun execute() {
        val cargoSpeed = -cargoSource()
        val hatchSpeed = -hatchSource()

        if (abs(cargoSpeed) > 0.2) {
            Intake.hatchMotorOutput = (-12).volt * cargoSpeed
            Intake.cargoMotorOutput = 12.volt * cargoSpeed
        } else {
            Intake.hatchMotorOutput = 12.volt * hatchSpeed
            Intake.cargoMotorOutput = 0.volt
        }
    }

    override fun end(interrupted: Boolean) {
        Intake.hatchMotorOutput = 0.volt
        Intake.cargoMotorOutput = 0.volt
    }

    companion object {
        val cargoSource by lazy { Controls.operatorFalconHID.getRawAxis(0) }
        val hatchSource by lazy { Controls.operatorFalconHID.getRawAxis(1) }
    }
}
*/

class IntakeCloseCommand : FalconCommand(Intake) {
    init {
        Intake.wantsOpen = false
    }
}
