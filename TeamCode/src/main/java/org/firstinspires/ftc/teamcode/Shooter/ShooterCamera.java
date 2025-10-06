
package org.firstinspires.ftc.teamcode.Shooter;

import android.util.Size;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;


import java.util.List;
import java.util.concurrent.TimeUnit;

public class ShooterCamera {

    // Odometry stuff -------------------

    private final AprilTagProcessor camera;
    private final VisionPortal visionPortal;
    private Telemetry telemetry;
    private double distanceToTag;
    private double tagElevation;
    boolean colorsAssigned;
    private int orderID;
    private int exposureMS = 10;

    Servo cameraMount;

    public ShooterCamera(Telemetry telemetryy, HardwareMap hardwaremap) {


        telemetry = telemetryy;
        camera = new AprilTagProcessor.Builder().build();
        VisionPortal.Builder builder = new VisionPortal.Builder();
        builder.setCamera(hardwaremap.get(WebcamName.class, "hsc"));
        builder.setCameraResolution(new Size(1280, 800));
        builder.setStreamFormat(VisionPortal.StreamFormat.MJPEG);
        builder.enableLiveView(false);
        builder.setAutoStartStreamOnBuild(true);
        builder.addProcessor(camera);

        visionPortal = builder.build();



        cameraMount = hardwaremap.get(Servo.class, "cameraServo");
        cameraMount.setPosition(.15);


        visionPortal.resumeStreaming();
        ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
        exposureControl.setMode(ExposureControl.Mode.Manual);
        exposureControl.setExposure((long)exposureMS, TimeUnit.MILLISECONDS);

        colorsAssigned = false;

    }


    public void alignCameraToTag() {

        AprilTagDetection lockedTag = getBasketDetection();
        double initialPos = cameraMount.getPosition();
        double targetPos;

        if (lockedTag != null) {

            targetPos = Range.clip((initialPos + (lockedTag.ftcPose.elevation/360)/4), 0, .5);

            cameraMount.setPosition(targetPos);

        }

    }
    public double alignRobotToTagPower() {
        AprilTagDetection lockedTag = getBasketDetection();
        double tolerance = 1; // degrees

        if (lockedTag == null) return 0;

        double bearingDifference = lockedTag.ftcPose.bearing;

        if (bearingDifference > tolerance) return 0.085;
        if (bearingDifference < -tolerance) return -0.085;

        return 0;
    }



    public AprilTagDetection getBasketDetection() {
        List<AprilTagDetection> currentDetections = camera.getDetections();
        for (AprilTagDetection detection : currentDetections) {
            if (detection.id == 24 || detection.id == 20) {
                return detection;
            }
            if (detection.id == 21 || detection.id == 22 || detection.id == 23) {
                orderID = detection.id;
                return null;
            }
            return null; // Return null if the tag is not found
        }
        return null; // Return null if the tag is not found

    }


    //20 (Blue)	-1.482m	-1.413m	0.749m	54°
    //24 (Red)	-1.482m	1.413m	0.749m	-54°

    //20 (Blue) -58.35in    -55.63in    29.49in    54°
    //24 (Red)  -58.35in     55.63in    29.49n   -54°

    private static final double[] TAG_20_POSE = {-58.35, -55.63, 54.0};
    private static final double[] TAG_24_POSE = {-58.35, 55.63, -54.0};

    //rotate and whereami

    public int getID() {
        return camera.getDetections().get(0).id;
    }

    public int getOrderID() { return orderID; }
    public double getTagElevation() { return tagElevation; }
    public double getFromTagDistance() { return distanceToTag; }

    public boolean canSeeRedTag() {
        return (getBasketDetection() != null && getBasketDetection().id == 24);
    }


    public void addTelemetry() {
        telemetry.addData("Distance to apriltag", distanceToTag);
        telemetry.addData("Visible Tags", camera.getDetections().size());
    }
}
