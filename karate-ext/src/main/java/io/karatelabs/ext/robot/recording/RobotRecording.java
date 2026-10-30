package io.karatelabs.ext.robot.recording;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record RobotRecording(io.karatelabs.ext.robot.recording.RobotRecording.RecordingMetadata a, String b, io.karatelabs.ext.robot.recording.RobotRecording.ApplicationEntryPage c, List<io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage> d, List<String> e, List<io.karatelabs.ext.robot.recording.RobotRecording.TypedInputVariable> f, List<io.karatelabs.ext.robot.recording.RobotRecording.RecordedCookie> g, io.karatelabs.ext.robot.recording.RobotRecording.AuthenticationProfile h, List<io.karatelabs.ext.robot.recording.RobotRecording.ActionGroup> i, List<io.karatelabs.ext.robot.recording.RobotRecording.RecordedEndpoint> j, List<io.karatelabs.ext.robot.recording.RobotRecording.RecordingWarning> k, List<io.karatelabs.ext.robot.recording.RobotRecording.RecordingGap> l, List<io.karatelabs.ext.robot.recording.RobotRecording.PatternEvidence> m, List<io.karatelabs.ext.robot.recording.RobotRecording.PatternVariable> n) {


   public static io.karatelabs.ext.robot.recording.RobotRecording a(
      io.karatelabs.ext.robot.recording.TrafficLog var0, Map<String, Object> var1, String var2, Map<String, Object> var3, Collection<String> var4, Map<String, Object> var5
   ) {
      return new io.karatelabs.ext.robot.recording.RecordingAnalyzer(var0, var1, var2, var3, var4, var5).a();
   }

   public static io.karatelabs.ext.robot.recording.RobotRecording a(
      io.karatelabs.ext.robot.recording.TrafficLog var0, Map<String, Object> var1, String var2, Map<String, Object> var3, Collection<String> var4
   ) {
      return a(var0, var1, var2, var3, var4, null);
   }

   public record RecordedAction(String a, Map<String, Object> b, String c, Object d, Map<String, Object> e, io.karatelabs.ext.robot.recording.RobotRecording.RecordingPhase f, boolean g, String h, List<String> i, List<io.karatelabs.ext.robot.recording.RobotRecording.RequestValueEvidence> j, io.karatelabs.ext.robot.recording.RobotRecording.ResponseExpectation k, io.karatelabs.ext.robot.recording.RobotRecording.CaptureStats l, List<io.karatelabs.ext.robot.recording.RobotRecording.ResponseFieldCapture> m) {

   }

   public record RecordedEndpoint(String a, io.karatelabs.ext.robot.recording.RobotRecording.NamedValue b, List<io.karatelabs.ext.robot.recording.RobotRecording.NamedValue> c, String d, String e, List<io.karatelabs.ext.robot.recording.RobotRecording.NamedValue> f) {

   }

   public record RecordedCookie(String a, String b) {

   }

   public record ResponseFieldCapture(String a, String b, String c) {

   }

   public record ResponseExpectation(Object a, Map<String, String> b, boolean c, boolean d, List<String> e) {

   }

   public record RecordingWarning(String a, String b, String c) {

   }

   public record RecordedPage(String a, String b, String c, List<String> d) {

   }

   public static enum ValueSourceKind {
      CHAIN,
      NEEDS,
      TYPED,
      COOKIE,
      AUTH,
      LITERAL;
   }

   public record PatternVariable(String a, String b, String c) {

   }

   public record ApplicationEntryPage(String a, String b, String c) {

   }

   public record AuthenticationProfile(String a, boolean b) {

   }

   public record PatternEvidence(String a, String b, String c, int d, String e, List<String> f, String g) {

   }

   public record NamedValue(String a, String b) {

   }

   public record RecordingMetadata(String a, int b, String c) {

   }

   public record RequestValueEvidence(String a, String b, String c, String d, String e, io.karatelabs.ext.robot.recording.RobotRecording.ValueSourceKind f, String g, io.karatelabs.ext.robot.recording.RobotRecording.NamedValue h, String i, String j) {

   }

   public record CaptureStats(int a, int b, Object c, String d) {

   }

   public record ActionGroup(int a, List<io.karatelabs.ext.robot.recording.RobotRecording.RecordedAction> b) {

   }

   public static enum RecordingPhase {
      SIGN_IN,
      PAGE_OPENED,
      AFTER_ACTION;
   }

   public record TypedInputVariable(String a, Object b, Object c, boolean d) {

   }

   public record RecordingGap(String a, String b, String c) {

   }
}
