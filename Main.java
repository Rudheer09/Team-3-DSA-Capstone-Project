import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    static class RadixNode {
        String edgeLabel;
        boolean isEndPattern;
        Map<Character, RadixNode> children;

        public RadixNode(String edgeLabel, boolean isEndPattern) {
            this.edgeLabel = edgeLabel;
            this.isEndPattern = isEndPattern;
            this.children = new HashMap<>();
        }
    }

    private final RadixNode root;

    public Main() {
        this.root = new RadixNode("", false);
    }

    public void insert(String pattern) {
        RadixNode current = root;
        int i = 0;
        
        while (i < pattern.length()) {
            char firstChar = pattern.charAt(i);
            String remaining = pattern.substring(i);
            
            if (!current.children.containsKey(firstChar)) {
                current.children.put(firstChar, new RadixNode(remaining, true));
                break;
            }
            
            RadixNode child = current.children.get(firstChar);
            String edge = child.edgeLabel;
            
            int commonLength = 0;
            while (commonLength < remaining.length() && commonLength < edge.length() 
                   && remaining.charAt(commonLength) == edge.charAt(commonLength)) {
                commonLength++;
            }
            
            if (commonLength < edge.length()) {
                String commonPrefix = edge.substring(0, commonLength);
                String edgeSuffix = edge.substring(commonLength);
                
                RadixNode splitNode = new RadixNode(commonPrefix, false);
                current.children.put(firstChar, splitNode);
                
                child.edgeLabel = edgeSuffix;
                splitNode.children.put(edgeSuffix.charAt(0), child);
                
                if (commonLength < remaining.length()) {
                    splitNode.children.put(remaining.charAt(commonLength), 
                        new RadixNode(remaining.substring(commonLength), true));
                } else {
                    splitNode.isEndPattern = true;
                }
                break;
            }
            
            current = child;
            i += commonLength;
        }
    }

    public void searchGenome(String query) {
        long startTime = System.nanoTime();
        
        RadixNode current = root;
        int i = 0;
        
        while (i < query.length()) {
            char firstChar = query.charAt(i);
            if (!current.children.containsKey(firstChar)) {
                printMutationResult(query, remainingString(query, i), startTime);
                return;
            }
            
            RadixNode child = current.children.get(firstChar);
            String edge = child.edgeLabel;
            String remaining = query.substring(i);
            
            int matchLen = 0;
            while (matchLen < remaining.length() && matchLen < edge.length() 
                   && remaining.charAt(matchLen) == edge.charAt(matchLen)) {
                matchLen++;
            }
            
            if (matchLen < edge.length() && matchLen == remaining.length()) {
                printExactResult("[✨ SUBSTRING MATCH]: Partial sequence found within the prefix index tree.", startTime);
                return;
            } else if (matchLen < edge.length()) {
                printMutationResult(query, remaining, startTime);
                return;
            }
            
            current = child;
            i += matchLen;
        }
        printExactResult("[✨ EXACT MATCH]: Target sequence fully verified in genome database.", startTime);
    }

    private String remainingString(String q, int index) {
        return index < q.length() ? q.substring(index) : "";
    }

    private void printExactResult(String message, long startTime) {
        double timeMs = (System.nanoTime() - startTime) / 1000000.0;
        System.out.println(message);
        System.out.printf(" -> Response Latency: %.3f ms\n", timeMs);
    }

    private void printMutationResult(String target, String mismatchedPart, long startTime) {
        double timeMs = (System.nanoTime() - startTime) / 1000000.0;
        System.out.println("[⚠️ MUTATION DETECTED]: Mismatch sequence branch found.");
        System.out.println(" -> Analyzed Segment: " + target);
        System.out.println(" -> Discrepancy Point: \"" + mismatchedPart + "\" deviations detected.");
        System.out.println(" -> Action: Flagging sample for Single Nucleotide Polymorphism (SNP) review.");
        System.out.printf(" -> Response Latency: %.3f ms\n", timeMs);
    }

    public static void main(String[] args) {
        Main genomeDB = new Main();
        Scanner scanner = new Scanner(System.in);

        System.out.println("==================================================");
        System.out.println("   🧬 GENOME SEQUENCE RETRIEVAL SYSTEM 🧬         ");
        System.out.println("==================================================");
        System.out.println("Indexing Reference DNA Strands...");
        
        genomeDB.insert("ACGTACGTTAG");
        genomeDB.insert("ACGTAAA");
        genomeDB.insert("TGCA");
        genomeDB.insert("GATTACA");
        
        System.out.println("[Status] Database Online. Memory Optimized via Node Compression.\n");

        while (true) {
            System.out.println("--------------------------------------------------");
            System.out.print("Enter DNA Sequence to query (or type 'EXIT' to quit): ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.equals("EXIT")) {
                System.out.println("\nShutting down Genome Retrieval System... Goodbye!");
                break;
            }

            if (input.isEmpty()) {
                System.out.println("[!] Please enter a valid sequence.");
                continue;
            }

            System.out.println("\nExecuting Scan for: \"" + input + "\"");
            genomeDB.searchGenome(input);
        }
        
        scanner.close();
    }
}