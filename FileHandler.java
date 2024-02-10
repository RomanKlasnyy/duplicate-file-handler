import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class FileHandler {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the parent directory path:");
        String path = scanner.nextLine();
        File directory = new File(path);

        if (directory.exists() && directory.isDirectory()) {
            FileSorter fileSorter = new FileSorter();
            Files myFiles = fileSorter.osWalk(directory);

            Comparator<Long> comparator = fileSorter.getSortingOption();
            List<Long> sortedKeys = new ArrayList<>(myFiles.getFileDict().keySet());
            sortedKeys.sort(comparator);

            for (Long key : sortedKeys) {
                System.out.println(key + " bytes");
                List<String> files = myFiles.getFileDict().get(key);
                for (String file : files) {
                    System.out.println(file);
                }
                System.out.println();
            }

            if (yesNoQuestion("Check for duplicates?")) {
                myFiles.printDuplicates(sortedKeys);
            }

            if (yesNoQuestion("Delete files?")) {
                List<Integer> fileNums = numsList("Enter file numbers to delete:");
                long freed = myFiles.delete(fileNums);
                System.out.println("Total freed up space: " + freed + " bytes");
            }
        } else {
            System.out.println("Directory is not specified or does not exist.");
        }
    }

    public static boolean yesNoQuestion(String question) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println(question);
            String userInput = scanner.nextLine();
            if (userInput.equals("yes")) {
                return true;
            } else if (userInput.equals("no")) {
                return false;
            } else {
                System.out.println("Wrong option");
            }
        }
    }

    public static List<Integer> numsList(String question) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println(question);
            String[] fileNumsStr = scanner.nextLine().split(" ");
            List<Integer> fileNums = new ArrayList<>();
            try {
                for (String numStr : fileNumsStr) {
                    fileNums.add(Integer.parseInt(numStr));
                }
                if (!fileNums.isEmpty()) {
                    return fileNums;
                } else {
                    System.out.println("Wrong format");
                }
            } catch (NumberFormatException e) {
                System.out.println("Wrong format");
            }
        }
    }
}

class FileSorter {
    private final Map<Integer, Comparator<Long>> sortingOptions = new HashMap<>();

    public FileSorter() {
        sortingOptions.put(1, Comparator.reverseOrder());
        sortingOptions.put(2, Comparator.naturalOrder());
    }

    public Comparator<Long> getSortingOption() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a sorting option:");
        while (true) {
            int option = scanner.nextInt();
            if (sortingOptions.containsKey(option)) {
                return sortingOptions.get(option);
            } else {
                System.out.println("Wrong option");
            }
        }
    }

    public Files osWalk(File startDirectory) {
        Map<Long, List<String>> fileDict = new HashMap<>();
        File[] files = startDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    long fileSize = file.length();
                    fileDict.computeIfAbsent(fileSize, k -> new ArrayList<>()).add(file.getAbsolutePath());
                }
            }
        }
        return new Files(fileDict);
    }
}

class Files {
    private final Map<Long, List<String>> fileDict;
    private final Map<Integer, Map<String, List<String>>> hashDict;
    private final Map<Integer, Map<String, Long>> idDuplicate;

    public Files(Map<Long, List<String>> fileDict) {
        this.fileDict = fileDict;
        this.hashDict = checkDuplicates();
        this.idDuplicate = new HashMap<>();
    }

    public Map<Long, List<String>> getFileDict() {
        return fileDict;
    }

    public Map<Integer, Map<String, List<String>>> checkDuplicates() {
        Map<Integer, Map<String, List<String>>> hashDict = new HashMap<>();
        for (Long size : fileDict.keySet()) {
            Map<String, List<String>> hashList = new HashMap<>();
            for (String fileName : fileDict.get(size)) {
                String md5Hash = md5(fileName);
                hashList.computeIfAbsent(md5Hash, k -> new ArrayList<>()).add(fileName);
            }
            hashDict.put(size.intValue(), hashList);
        }
        return hashDict;
    }

    public void printDuplicates(List<Long> sizes) {
        int i = 1;
        for (long size : sizes) {
            System.out.println(size + " bytes");
            for (String md5Hash : hashDict.get((int) size).keySet()) {
                List<String> files = hashDict.get((int) size).get(md5Hash);
                if (files.size() > 1) {
                    System.out.println("Hash: " + md5Hash);
                    for (String file : files) {
                        idDuplicate.put(i, Map.of());
                        System.out.println(i + ". " + file);
                        i++;
                    }
                }
            }
            System.out.println();
        }
    }

    public long delete(List<Integer> ids) {
        long freed = 0;
        for (int id : ids) {
            Long file = idDuplicate.get(id).get("file");
            long size = idDuplicate.get(id).get("size");
            File f = new File(String.valueOf(file));
            if (f.delete()) {
                freed += size;
            }
        }
        return freed;
    }

    public String md5(String fileName) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(fileName)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
                byte[] digest = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
