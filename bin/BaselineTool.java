import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Minimal output-baseline tool for M4 step 9 (DEVELOPMENT_PLAN Phase H5).
 *
 * Covers the deterministic text outputs (UTF-8 CSVs and parameters.json), which
 * are the strongest byte-identical signal. Binary outputs (multi-page TIFF
 * stacks, PNGs) and labels.zip are intentionally excluded for now: their
 * comparison needs pixel-payload / sorted-ROI normalisation rather than a raw
 * file hash.
 *
 * Usage:
 *   compute <outputDir> <manifestFile>   write relative-path + SHA-256 lines
 *   verify  <outputDir> <manifestFile>   compare and exit non-zero on mismatch
 */
public class BaselineTool {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: BaselineTool compute|verify <outputDir> <manifestFile>");
            System.exit(2);
        }
        String mode = args[0];
        Path outDir = Paths.get(args[1]);
        Path manifest = Paths.get(args[2]);
        if ("compute".equals(mode)) {
            compute(outDir, manifest);
        } else if ("verify".equals(mode)) {
            verify(outDir, manifest);
        } else {
            System.err.println("Unknown mode: " + mode);
            System.exit(2);
        }
    }

    private static List<Path> collect(Path dir) throws Exception {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.walk(dir)) {
            s.filter(Files::isRegularFile)
                    .filter(BaselineTool::isDeterministic)
                    .sorted()
                    .forEach(files::add);
        }
        return files;
    }

    private static boolean isDeterministic(Path p) {
        String n = p.getFileName().toString();
        return n.endsWith(".csv") || n.equals("parameters.json");
    }

    private static String sha256(Path f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(f)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String rel(Path outDir, Path f) {
        return outDir.relativize(f).toString().replace('\\', '/');
    }

    private static void compute(Path outDir, Path manifest) throws Exception {
        List<Path> files = collect(outDir);
        StringBuilder sb = new StringBuilder();
        for (Path f : files) {
            sb.append(rel(outDir, f)).append("  ").append(sha256(f)).append('\n');
        }
        Files.writeString(manifest, sb.toString());
        System.out.println("Wrote " + manifest + " (" + files.size() + " files)");
    }

    private static void verify(Path outDir, Path manifest) throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        for (String line : Files.readAllLines(manifest)) {
            int idx = line.indexOf("  ");
            if (idx < 0) {
                continue;
            }
            expected.put(line.substring(0, idx), line.substring(idx + 2).trim());
        }
        List<Path> files = collect(outDir);
        int ok = 0;
        int fail = 0;
        int missing = 0;
        Set<String> seen = new HashSet<>();
        for (Path f : files) {
            String r = rel(outDir, f);
            seen.add(r);
            String exp = expected.get(r);
            if (exp == null) {
                System.out.println("UNEXPECTED " + r);
                fail++;
                continue;
            }
            String got = sha256(f);
            if (got.equals(exp)) {
                ok++;
            } else {
                System.out.println("MISMATCH   " + r);
                fail++;
            }
        }
        for (String r : expected.keySet()) {
            if (!seen.contains(r)) {
                System.out.println("MISSING    " + r);
                missing++;
            }
        }
        System.out.println("ok=" + ok + " fail=" + fail + " missing=" + missing);
        if (fail > 0 || missing > 0) {
            System.exit(1);
        }
        System.out.println("BASELINE_PASS");
    }
}
