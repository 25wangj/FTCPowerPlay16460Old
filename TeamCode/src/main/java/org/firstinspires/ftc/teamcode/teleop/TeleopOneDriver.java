package org.firstinspires.ftc.teamcode.teleop;
import static java.lang.Math.*;
import static com.qualcomm.robotcore.util.Range.*;
import static org.firstinspires.ftc.teamcode.classes.ValueStorage.*;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.classes.ProfileChain;
import org.firstinspires.ftc.teamcode.classes.Robot;
import org.firstinspires.ftc.teamcode.classes.ValueStorage;
@TeleOp(name = "TeleopOneDriver")
public class TeleopOneDriver extends LinearOpMode {
    Robot robot = new Robot();
    int state = 0;
    int holderDetectionCount = 0;
    double increment = 0.005;
    double initialHeading = ValueStorage.lastPose.getHeading() - side * PI / 2;
    double robotHeading;
    double moveAngle;
    double moveMagnitude;
    double turn;
    double stateTime = 0;
    double time;
    boolean stateDir = true;
    boolean aPressed = false;
    boolean bPressed = false;
    boolean aReleased = true;
    boolean bReleased = true;
    boolean xPressed = false;
    boolean xReleased = false;
    boolean yPressed = false;
    boolean yReleased = false;
    boolean lbPressed = false;
    boolean lbReleased = false;
    boolean rbPressed = false;
    boolean rbReleased = false;
    ElapsedTime clock = new ElapsedTime();
    @Override
    public void runOpMode() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        robot.init(hardwareMap, armRest, wristRest);
        robot.gripper.setPosition(gripperRelease);
        robot.retract.setPosition(odoUp);
        robot.roller.setPosition(rollerDown);
        while (!isStarted() && !isStopRequested()) {
            robot.update(clock.seconds());
        }
        while (opModeIsActive() && !isStopRequested()) {
            if (gamepad1.a) {
                aPressed = aReleased;
                aReleased = false;
            } else {
                aPressed = false;
                aReleased = true;
            }
            if (gamepad1.b) {
                bPressed = bReleased;
                bReleased = false;
            } else {
                bPressed = false;
                bReleased = true;
            }
            if (gamepad1.x) {
                xPressed = xReleased;
                xReleased = false;
            } else {
                xPressed = false;
                xReleased = true;
            }
            if (gamepad1.y) {
                yPressed = yReleased;
                yReleased = false;
            } else {
                yPressed = false;
                yReleased = true;
            }
            if (gamepad1.left_bumper) {
                lbPressed = lbReleased;
                lbReleased = false;
            } else {
                lbPressed = false;
                lbReleased = true;
            }
            if (gamepad1.right_bumper) {
                rbPressed = rbReleased;
                rbReleased = false;
            } else {
                rbPressed = false;
                rbReleased = true;
            }
            if (gamepad1.ps) {
                initialHeading -= robotHeading;
            }
            if (robot.holder.getDistance(DistanceUnit.INCH) < holderDetectionThreshold) {
                holderDetectionCount++;
            } else {
                holderDetectionCount = 0;
            }
            time = clock.seconds();
            switch (state) {
                case 0:
                    if (time < stateTime) {
                        if (!stateDir && time > robot.armTime()) {
                            robot.gripper.setPosition(gripperRelease);
                        }
                    } else {
                        if (gamepad1.right_trigger < 0.2) {
                            robot.setIntakePowers(1, 1);
                        } else if (gamepad1.right_trigger < 0.8) {
                            robot.setIntakePowers(0, 0);
                        } else {
                            robot.setIntakePowers(-0.5, -0.5);
                        }
                        if (gamepad1.left_trigger < 0.2) {
                            robot.roller.setPosition(rollerDown);
                        } else if (gamepad1.left_trigger < 0.8) {
                            robot.roller.setPosition(scale(gamepad1.left_trigger, 0.2, 0.8, rollerDown, rollerUp));
                        } else {
                            robot.roller.setPosition(rollerUp);
                        }
                        if (rbPressed || holderDetectionCount >= holderMinCount) {
                            state = 1;
                            stateDir = true;
                            robot.setIntakePowers(-0.5, -0.5);
                            robot.armProfile = forwardArmProfile1(time);
                            robot.wristProfile = forwardWristProfile1(time);
                            stateTime = robot.armTime();
                            robot.gripper.setPosition(gripperHold);
                            robot.roller.setPosition(rollerRetract);
                        } else if (lbPressed) {
                            state = 4;
                            stateDir = false;
                            stateTime = time;
                            robot.setIntakePowers(0, 0);
                            robot.roller.setPosition(rollerRetract);
                        }
                    }
                    break;
                case 1:
                    if (time > stateTime) {
                        robot.setIntakePowers(0, 0);
                        if (lbPressed) {
                            state = 0;
                            stateDir = false;
                            robot.armProfile = backArmProfile1(time);
                            robot.wristProfile = backWristProfile1(time);
                            stateTime = robot.armTime() + 0.25;
                            robot.roller.setPosition(rollerDown);
                        }
                    }
                    if (aPressed) {
                        double readyTime = max(time, ((ProfileChain) robot.armProfile).getProfiles().get(0).getTf());
                        double upTime = max(time, stateTime);
                        state = 2;
                        robot.extendLiftProfile(readyTime, liftLowClose[0], 0);
                        robot.armProfile = ((ProfileChain) robot.armProfile)
                                .addExtendTrapezoidal(armMaxVel, armMaxAccel, upTime, liftLowClose[1], 0);
                        robot.wristProfile = ((ProfileChain) robot.wristProfile)
                                .addExtendTrapezoidal(wristMaxVel, wristMaxAccel, upTime, liftLowClose[2], 0);
                        stateTime = robot.restTime();
                        robot.setIntakePowers(0, 0);
                    } else if (bPressed) {
                        double readyTime = max(time, ((ProfileChain) robot.armProfile).getProfiles().get(0).getTf());
                        double upTime = max(time, stateTime);
                        state = 2;
                        robot.extendLiftProfile(readyTime, liftMedClose[0], 0);
                        robot.armProfile = ((ProfileChain) robot.armProfile)
                                .addExtendTrapezoidal(armMaxVel, armMaxAccel, upTime, liftMedClose[1], 0);
                        robot.wristProfile = ((ProfileChain) robot.wristProfile)
                                .addExtendTrapezoidal(wristMaxVel, wristMaxAccel, upTime, liftMedClose[2], 0);
                        stateTime = robot.restTime();
                        robot.setIntakePowers(0, 0);
                    } else if (gamepad1.right_trigger > 0.2 && yPressed) {
                        double readyTime = max(time, ((ProfileChain) robot.armProfile).getProfiles().get(0).getTf());
                        double upTime = max(time, stateTime);
                        state = 2;
                        robot.extendLiftProfile(readyTime, liftHighFar[0], 0);
                        robot.armProfile = ((ProfileChain) robot.armProfile)
                                .addExtendTrapezoidal(armMaxVel, armMaxAccel, upTime, liftHighFar[1], 0);
                        robot.wristProfile = ((ProfileChain) robot.wristProfile)
                                .addExtendTrapezoidal(wristMaxVel, wristMaxAccel, upTime, liftHighFar[2], 0);
                        stateTime = robot.restTime();
                        robot.setIntakePowers(0, 0);
                    } else if (yPressed) {
                        double readyTime = max(time, ((ProfileChain) robot.armProfile).getProfiles().get(0).getTf());
                        double upTime = max(time, stateTime);
                        state = 2;
                        robot.extendLiftProfile(readyTime, liftHighClose[0], 0);
                        robot.armProfile = ((ProfileChain) robot.armProfile)
                                .addExtendTrapezoidal(armMaxVel, armMaxAccel, upTime, liftHighClose[1], 0);
                        robot.wristProfile = ((ProfileChain) robot.wristProfile)
                                .addExtendTrapezoidal(wristMaxVel, wristMaxAccel, upTime, liftHighClose[2], 0);
                        stateTime = robot.restTime();
                        robot.setIntakePowers(0, 0);
                    } else if (xPressed) {
                        double readyTime = max(time, ((ProfileChain) robot.armProfile).getProfiles().get(0).getTf());
                        double upTime = max(time, stateTime);
                        state = 2;
                        robot.extendLiftProfile(readyTime, liftGroundClose[0], 0);
                        robot.armProfile = ((ProfileChain) robot.armProfile)
                                .addExtendTrapezoidal(armMaxVel, armMaxAccel, upTime, liftGroundClose[1], 0);
                        robot.wristProfile = ((ProfileChain) robot.wristProfile)
                                .addExtendTrapezoidal(wristMaxVel, wristMaxAccel, upTime, liftGroundClose[2], 0);
                        stateTime = robot.restTime();
                        robot.setIntakePowers(0, 0);
                    }
                    break;
                case 2:
                    if (time > stateTime) {
                        if (aPressed) {
                            robot.extendLiftProfile(time, liftLowClose[0], 0);
                            robot.extendArmProfile(time, liftLowClose[1], 0);
                            robot.extendWristProfile(time, liftLowClose[2], 0);
                            stateTime = robot.restTime();
                        } else if (bPressed) {
                            robot.extendLiftProfile(time, liftMedClose[0], 0);
                            robot.extendArmProfile(time, liftMedClose[1], 0);
                            robot.extendWristProfile(time, liftMedClose[2], 0);
                            stateTime = robot.restTime();
                        } else if (gamepad1.right_trigger > 0.2 && yPressed) {
                            robot.extendLiftProfile(time, liftHighFar[0], 0);
                            robot.extendArmProfile(time, liftHighFar[1], 0);
                            robot.extendWristProfile(time, liftHighFar[2], 0);
                            stateTime = robot.restTime();
                        } else if (yPressed) {
                            robot.extendLiftProfile(time, liftHighClose[0], 0);
                            robot.extendArmProfile(time, liftHighClose[1], 0);
                            robot.extendWristProfile(time, liftHighClose[2], 0);
                            stateTime = robot.restTime();
                        } else if (xPressed) {
                            robot.extendLiftProfile(time, liftGroundClose[0], 0);
                            robot.extendArmProfile(time, liftGroundClose[1], 0);
                            robot.extendWristProfile(time, liftGroundClose[2], 0);
                            stateTime = robot.restTime();
                        } else if (rbPressed) {
                            state = 3;
                            stateDir = true;
                            stateTime = time + 0.5;
                            robot.gripper.setPosition(gripperRelease);
                        }
                    }
                    break;
                case 3:
                    if (time > stateTime) {
                        if (rbPressed) {
                            state = 4;
                            robot.extendLiftProfile(time, 0, 0);
                            robot.extendArmProfile(time, armIn, 0);
                            robot.extendWristProfile(time, wristIn, 0);
                            stateTime = robot.restTime();
                        } else if (lbPressed) {
                            state = 2;
                            stateDir = false;
                            stateTime = time + 0.5;
                            robot.gripper.setPosition(gripperHold);
                        }
                    }
                    break;
                case 4:
                    if (time < stateTime) {
                        if (rbPressed && stateDir) {
                            state = 0;
                            double downTime = robot.restTime();
                            robot.armProfile = new ProfileChain(robot.armProfile)
                                    .add(forwardArmProfile2(downTime));
                            robot.wristProfile = new ProfileChain(robot.wristProfile)
                                    .add(forwardWristProfile2(downTime));
                            stateTime = robot.armTime();
                            robot.roller.setPosition(rollerDown);
                        }
                    } else if (rbPressed) {
                        state = 0;
                        if (stateDir) {
                            robot.armProfile = forwardArmProfile2(time);
                            robot.wristProfile = forwardWristProfile2(time);
                            stateTime = robot.armTime();
                        } else {
                            stateTime = time;
                        }
                        stateDir = true;
                        robot.roller.setPosition(rollerDown);
                    }
                    break;
            }
            if (gamepad1.dpad_up && (state == 2 || state == 3) && time > stateTime) {
                robot.extendLiftProfile(time, adjust(robot.liftProfile.getX(time), increment)[0], 0);
                robot.extendArmProfile(time, adjust(robot.liftProfile.getX(time), increment)[1], 0);
                robot.extendWristProfile(time, adjust(robot.liftProfile.getX(time), increment)[2], 0);
            } else if (gamepad1.dpad_down && (state == 2 || state == 3) && time > stateTime) {
                robot.extendLiftProfile(time, adjust(robot.liftProfile.getX(time), -increment)[0], 0);
                robot.extendArmProfile(time, adjust(robot.liftProfile.getX(time), -increment)[1], 0);
                robot.extendWristProfile(time, adjust(robot.liftProfile.getX(time), -increment)[2], 0);
            }
            robot.update(time);
            robotHeading = robot.heading() + initialHeading;
            moveAngle = atan2(-gamepad1.left_stick_x, -gamepad1.left_stick_y) - robotHeading;
            moveMagnitude = pow(pow(gamepad1.left_stick_x, 2) + pow(gamepad1.left_stick_y, 2), 1.5);
            if (moveMagnitude < 0.01) {
                moveMagnitude = 0;
            }
            turn = pow(gamepad1.right_stick_x, 3);
            robot.setDrivePowers(moveMagnitude * clip(sin(PI / 4 - moveAngle) / abs(cos(PI / 4 - moveAngle)), -1, 1) + turn,
                    moveMagnitude * clip(sin(PI / 4 + moveAngle) / abs(cos(PI / 4 + moveAngle)), -1, 1) - turn,
                    moveMagnitude * clip(sin(PI / 4 + moveAngle) / abs(cos(PI / 4 + moveAngle)), -1, 1) + turn,
                    moveMagnitude * clip(sin(PI / 4 - moveAngle) / abs(cos(PI / 4 - moveAngle)), -1, 1) - turn);
        }
    }
}