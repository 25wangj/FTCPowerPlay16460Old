package org.firstinspires.ftc.teamcode.classes;
import static java.lang.Math.*;
import static org.firstinspires.ftc.teamcode.classes.ValueStorage.*;
import com.outoftheboxrobotics.photoncore.PhotonCore;
import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;
public class Robot {
    public SampleMecanumDrive drive;
    public DcMotorEx fl;
    public DcMotorEx fr;
    public DcMotorEx bl;
    public DcMotorEx br;
    public DcMotorEx intakeL;
    public DcMotorEx intakeR;
    public DcMotorEx liftL;
    public DcMotorEx liftR;
    public Servo gripper;
    public Servo retract;
    public Servo roller;
    public Servo angleL;
    public Servo angleR;
    public Servo armL;
    public Servo armR;
    public Servo wristL;
    public Servo wristR;
    public RevColorSensorV3 holder;
    public IMU gyro;
    PidfController liftPidf = new PidfController(liftKp, liftKi, liftKd) {
        @Override
        public double kf(double input) {
            return liftKf (input);
        }
    };
    public TrapezoidalProfile liftProfile = new TrapezoidalProfile(liftMaxVel, liftMaxAccel, 0, 0, 0, 0, 0);
    public MotionProfile armProfile;
    public MotionProfile wristProfile;
    public void init(HardwareMap hwMap, double armPos, double wristPos) {
        drive = new SampleMecanumDrive(hwMap);
        fl = hwMap.get(DcMotorEx.class, "fl");
        fr = hwMap.get(DcMotorEx.class, "fr");
        bl = hwMap.get(DcMotorEx.class, "bl");
        br = hwMap.get(DcMotorEx.class, "br");
        intakeL = hwMap.get(DcMotorEx.class, "intakeL");
        intakeR = hwMap.get(DcMotorEx.class, "intakeR");
        liftL = hwMap.get(DcMotorEx.class, "liftL");
        liftR = hwMap.get(DcMotorEx.class, "liftR");
        gripper = hwMap.get(Servo.class, "gripper");
        retract = hwMap.get(Servo.class, "retract");
        roller = hwMap.get(Servo.class, "roller");
        //angleL = hwMap.get(Servo.class, "angleL");
        //angleR = hwMap.get(Servo.class, "angleR");
        armL = hwMap.get(Servo.class, "armL");
        armR = hwMap.get(Servo.class, "armR");
        wristL = hwMap.get(Servo.class, "wristL");
        wristR = hwMap.get(Servo.class, "wristR");
        holder = hwMap.get(RevColorSensorV3.class, "holder");
        gyro = hwMap.get(IMU.class, "gyro");
        fl.setDirection(DcMotorSimple.Direction.REVERSE);
        bl.setDirection(DcMotorSimple.Direction.REVERSE);
        intakeR.setDirection(DcMotorSimple.Direction.REVERSE);
        liftR.setDirection(DcMotorSimple.Direction.REVERSE);
        fl.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        liftL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        liftR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        armProfile = new TrapezoidalProfile(armMaxVel, armMaxAccel, 0, armPos, 0, armPos, 0);
        wristProfile = new TrapezoidalProfile(wristMaxVel, wristMaxAccel, 0, wristPos, 0, wristPos, 0);
        PhotonCore.enable();
        IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.RIGHT,
                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD));
        gyro.initialize(parameters);
    }
    public double heading() {
        return gyro.getRobotOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS).firstAngle;
    }
    public double restTime() {
        return max(max(liftProfile.getTf(), armProfile.getTf()), wristProfile.getTf());
    }
    public double armTime() {
        return max(armProfile.getTf(), wristProfile.getTf());
    }
    public void update(double time) {
        liftPidf.set(liftProfile.getX(time));
        liftPidf.update(liftL.getCurrentPosition());
        liftL.setPower(liftPidf.get());
        liftR.setPower(liftPidf.get());
        armL.setPosition(armProfile.getX(time));
        armR.setPosition(armOffset - armProfile.getX(time));
        wristL.setPosition(wristProfile.getX(time));
        wristR.setPosition(wristOffset - wristProfile.getX(time));
    }
    public void setDrivePowers(double flPower, double frPower, double blPower, double brPower) {
        fl.setPower(flPower);
        fr.setPower(frPower);
        bl.setPower(blPower);
        br.setPower(brPower);
    }
    public void setIntakePowers(double intakeLPower, double intakeRPower) {
        intakeL.setPower(intakeLPower);
        intakeR.setPower(intakeRPower);
    }
    public void extendLiftProfile(double t, double xf, double vf) {
        liftProfile = liftProfile.extendTrapezoidal(t, xf, vf);
    }
    public void extendArmProfile(double t, double xf, double vf) {
        armProfile = armProfile.extendTrapezoidal(armMaxVel, armMaxAccel, t, xf, vf);
    }
    public void extendWristProfile(double t, double xf, double vf) {
        wristProfile = wristProfile.extendTrapezoidal(wristMaxVel, wristMaxAccel, t, xf, vf);
    }
}
