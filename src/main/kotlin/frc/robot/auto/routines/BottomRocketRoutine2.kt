package frc.robot.auto.routines

import edu.wpi.first.wpilibj2.command.InstantCommand
import edu.wpi.first.wpilibj2.command.PrintCommand
import edu.wpi.first.wpilibj2.command.RunCommand
import edu.wpi.first.wpilibj2.command.WaitCommand
import frc.robot.auto.Autonomous
import frc.robot.auto.paths.TrajectoryFactory
import frc.robot.auto.paths.TrajectoryWaypoints
import frc.robot.subsystems.drive.DriveSubsystem
import frc.robot.subsystems.drive.TurnInPlaceCommand
import frc.robot.subsystems.intake.Intake
import frc.robot.subsystems.intake.IntakeHatchCommand
import frc.robot.subsystems.superstructure.Superstructure
import frc.robot.vision.TargetTracker
import org.ghrobotics.lib.commands.sequential
import org.ghrobotics.lib.mathematics.twodim.trajectory.types.duration
import org.ghrobotics.lib.mathematics.units.* // ktlint-disable no-wildcard-imports
import org.ghrobotics.lib.commands.* // ktlint-disable no-wildcard-imports
import org.ghrobotics.lib.mathematics.twodim.control.TrajectoryTracker
import org.ghrobotics.lib.mathematics.twodim.geometry.Pose2d
import org.ghrobotics.lib.mathematics.twodim.geometry.Rotation2d
import org.ghrobotics.lib.mathematics.units.derived.degree
import org.ghrobotics.lib.mathematics.units.derived.toRotation2d
import org.ghrobotics.lib.mathematics.units.derived.volt

class BottomRocketRoutine2 : AutoRoutine() {

    private val path1 = TrajectoryFactory.sideStartReversedToRocketFPrepare
    private val path2 = TrajectoryFactory.rocketFPrepareToRocketF

    // Second path goes to the loading station to pick up a hatch panel
    private val path3 = TrajectoryFactory.rocketFToRocketFPrepare
    private val path4 = TrajectoryFactory.rocketFPrepareToLoadingStation

    // Third path goes to the near side of the rocket
    private val path5 = TrajectoryFactory.loadingStationReversedToRocketNPrep
    private val path6 = TrajectoryFactory.rocketNPrepToRocketN

    override val duration: SIUnit<Second>
        get() = path4.duration + path5.duration + path1.duration + path2.duration

    override val routine
        get() = sequential {

            +PrintCommand("Starting")
            +InstantCommand(Runnable { DriveSubsystem.lowGear = false })

            +parallel {
                +DriveSubsystem.followTrajectory(
                        path1,
                        Autonomous.isStartingOnLeft
                )
                +(sequential {
                    +DriveSubsystem.notWithinRegion(TrajectoryWaypoints.kHabitatL1Platform)
                    +WaitCommand(0.5)
                    +Superstructure.kMatchStartToStowed
                }).beforeStarting { Intake.hatchMotorOutput = 6.volt }.whenFinished { Intake.hatchMotorOutput = 0.volt }
            }

            +TurnInPlaceCommand {
                Pose2d().mirror
//                val goal = TrajectoryWaypoints.kRocketF.translation.let { if(Autonomous.isStartingOnLeft()) it.mirror else it }
                val goalTarget = TargetTracker.getBestTarget(true)
                if(goalTarget != null) {
                    val goal = goalTarget.averagedPose2d.translation
                    val error = (goal - DriveSubsystem.robotPosition.translation)
                    Rotation2d(error.x.meter, error.y.meter, true)
                } else {
                    -151.degree.toRotation2d()
                }
//                -151.degree.toRotation2d()
            }

            +super.followVisionAssistedTrajectory(
                    path2,
                    Autonomous.isStartingOnLeft,
                    10.feet,
                    true
            )

            // Reorient position on field based on Vision alignment.
            +relocalize(
                    TrajectoryWaypoints.kRocketF,
                    true,
                    Autonomous.isStartingOnLeft,
                    isStowed = true
            )

            val spline3 = DriveSubsystem.followTrajectory(
                    path3,
                    Autonomous.isStartingOnLeft
            )
            val spline4 = super.followVisionAssistedTrajectory(
                    path4,
                    Autonomous.isStartingOnLeft,
                    6.feet, false
            )

            // Part 2: Place hatch and go to loading station.
            +parallel {
                // Follow the trajectory with vision correction to the loading station.
                +sequential {
                    +spline3
                    +spline4
                }
                // Take the superstructure to pickup position and arm hatch intake 3 seconds before arrival.
                +sequential {
                    // Place hatch panel.
                    +IntakeHatchCommand(true).withTimeout(1.second)
                    +WaitCommand(3.0)
                    +parallel {
//                        +Superstructure.kHatchLow
                        +IntakeHatchCommand(false).withExit { spline4.isFinished }
                    }
                }
            }

            // Reorient position on field based on Vision alignment.
            +relocalize(
                    TrajectoryWaypoints.kLoadingStationReversed,
                    true,
                    Autonomous.isStartingOnLeft,
                    isStowed = true
            )

            // Part 3: Pickup hatch and go to the near side of the rocket.
            +parallel {
                val path = DriveSubsystem.followTrajectory(path5, Autonomous.isStartingOnLeft)
                +path
                // Make sure the intake is holding the hatch panel.
                +IntakeHatchCommand(false).withTimeout(4.0.second).withExit { path.isFinished }
                // Follow the trajectory with vision correction to the near side of the rocket.
//                +WaitCommand(0.5)
//                +Superstructure.kStowed
            }
            // turn to face the goal
            +TurnInPlaceCommand {
//                val goal = TrajectoryWaypoints.kRocketN.translation.let { if(Autonomous.isStartingOnLeft()) it.mirror else it }
//                val error = (goal - DriveSubsystem.robotPosition.translation)
//                Rotation2d(error.x.meter, error.y.meter, true)
                (-28.75).degree.toRotation2d()
            }
            +followVisionAssistedTrajectory(
                    path6,
                    Autonomous.isStartingOnLeft,
                    4.feet
            )

            +parallel {
                +IntakeHatchCommand(true).withTimeout(1.0)
                +RunCommand(Runnable{ DriveSubsystem.tankDrive(-0.3, -0.3) }).withTimeout(1.0)
            }
        }
}