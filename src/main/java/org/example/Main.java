package org.example;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        Set<String> availabilities = new HashSet<>();
        availabilities.add("09:00-16:00 Royal Bank of Scotland");
        availabilities.add("11:00-17:00 Morgan Stanley");
        availabilities.add("14:00-20:00 JP Morgan");
        availabilities.add("02:00-07:00 National Australia Bank");

        TradingPlatform tradingPlatform = new TradingPlatform(availabilities);
        System.out.printf("10:00-17:00 is %s\n", tradingPlatform.queryAvailabilities("10:00-17:00") ? "available" : "not available");
        System.out.printf("15:00-21:00 is %s\n", tradingPlatform.queryAvailabilities("15:00-21:00") ? "available" : "not available");
    }
}

class TradingPlatform {
    private final TradingTimeStorage tradingTimeStorage = new TradingTimeStorage();

    public TradingPlatform(Set<String> bankAvailabilities) {
        for (String bankAvailability : bankAvailabilities) {
            tradingTimeStorage.addBankTimePeriod(bankAvailability);
        }
        // Debug
        // this.tradingTimeStorage.print(tradingTimeStorage.storage);
    }

    /**
     * Problem: Given a set of bank availabilities, which is a set of strings in the format of "HH:MM-HH:MM <Bank_Name>",
     * determine if a given time range is available.
     * @param time the time in format "HH:MM-HH:MM"
     * @return boolean whether the time is available
     * <p>
     * Example:
     * bankAvailabilities = {"09:00-16:00 Royal Bank of Scotland", "11:00-17:00 Morgan Stanley", "14:00-20:00 JP Morgan", "02:00-07:00 National Australia Bank"}
     * queryAvailabilities("10:00-17:00") = true
     * queryAvailabilities("15:00-21:00") = false
     * <p>
     * Draft solution with O(log(n)) time complexity:
     * 1. Convert the time range to minutes, e.g. "10:00-17:00" -> 600-1020
     * 2. Build a binary search tree where the key is the start time of the bank availability, and the value is the end time of the bank availability
     *  2.1 We need to ensure that the start time and end time represent unique interval in the tree, e.g. if we have 1000-1200 and 1100-1300, we need to merge them to 1000-1300
     * 3. Search the binary tree
     *  3.2 If we find the end time of a tree is smaller than start time of our query time, then we can safely move to the next node which has a latter time
     *  3.3 If we have reach to the end of the tree, output Not Available
     *  3.4 If any of the node's period is within our query time period, output Available
     */
    public boolean queryAvailabilities(String time) {
        List<Integer> timeRange = tradingTimeStorage.convertTimeToMinutes(time);
        return tradingTimeStorage.searchTimePeriod(timeRange.get(0), timeRange.get(1));
    }


    private static class TradingTimeStorage {
        private Node storage;
        /**
         * Utility Node class for the binary search tree
         * Note: Can be improved to RBTree to improve performance
         */
        private static class Node {
            public int startTime;
            public int endTime;
            private Node left;
            private Node right;

            public Node(int startTime, int endTime) {
                this.startTime = startTime;
                this.endTime = endTime;
            }
        }

        /**
         * Helper method to convert time string to minutes
         * @param time the time in format "HH:MM-HH:MM"
         * @return List<Integer> the start time and end time in minutes
         */
        private List<Integer> convertTimeToMinutes(String time) {
            String[] times = time.split("-");
            String[] startTimes = times[0].split(":");
            String[] endTimes = times[1].split(":");
            int startTime = Integer.parseInt(startTimes[0]) * 60 + Integer.parseInt(startTimes[1]);
            int endTime = Integer.parseInt(endTimes[0]) * 60 + Integer.parseInt(endTimes[1]);
            return List.of(startTime, endTime);
        }

        /**
         * Helper method to insert a new node into the binary search tree
         * @param timePeriod the time period in format "HH:MM-HH:MM <Bank_Name>"
         */
        public void addBankTimePeriod(String timePeriod) {
            String time = timePeriod.split(" ")[0];
            List<Integer> times = convertTimeToMinutes(time);
            int startTime = times.get(0);
            int endTime = times.get(1);

            if (storage == null) {
                storage = new Node(startTime, endTime);
                return;
            }

            Node current = storage;
            while (true) {
                // best case, we matched the start time
                if (startTime == current.startTime) {
                    current.endTime = Math.max(current.endTime, endTime);
                    return;
                }
                // if new start time is smaller than current start time, there are 4 cases:
                else if (startTime < current.startTime) {
                    // case1: node: (100, 200), our time: (50, 150) -> merge to (50, 200)
                    if (endTime <= current.endTime && endTime >= current.startTime) {
                        current.startTime = startTime;
                        return;
                    }
                    // case2: node: (100, 200), our time: (50, 250) -> merge to (50, 250)
                    if (endTime > current.endTime && endTime >= current.startTime) {
                        current.startTime = startTime;
                        current.endTime = endTime;
                        return;
                    }
                    // case3: node: (100, 200), our time: (50, 90) -> go the left node and continue
                    if (current.left != null) {
                        current = current.left;
                    }
                    // case4: left node is null -> insert to the left node
                    else {
                        current.left = new Node(startTime, endTime);
                        return;
                    }
                }
                // if new start time is greater than current start time
                else {
                    // case1: node: (100, 200), our time: (150, 250) -> merge to (100, 250)
                    if (startTime <= current.endTime && endTime >= current.endTime) {
                        current.endTime = endTime;
                        return;
                    }
                    // case2: node: (100, 200), our time: (150, 180) -> do nothing
                    if (startTime <= current.endTime) {
                        return;
                    }
                    // case3: node: (100, 200), our time: (250, 300) -> go the right node and continue
                    if (current.right != null) {
                        current = current.right;
                    }
                    // case4: right node is null -> insert to the right node
                    else {
                        current.right = new Node(startTime, endTime);
                        return;
                    }
                }
            }
        }

        /**
         * Helper method to search the binary search tree
         * @param startTime the start time in minutes
         * @param endTime the end time in minutes
         * @return boolean whether the time is available
         */
        public boolean searchTimePeriod(int startTime, int endTime) {
            Node current = storage;
            while (current != null) {
                // if the end time of the current node is smaller than the start time of our query time, then we can safely move to the next node which has a latter time
                if (current.endTime < startTime) {
                    current = current.right;
                }
                // if any of the node's period is within our query time period, output Available
                else if (current.startTime <= startTime && current.endTime >= endTime) {
                    return true;
                }
                // if the start time of the current node is greater than the end time of our query time, then we can safely move to the next node which has an earlier time
                else {
                    current = current.left;
                }
            }
            return false;
        }

        /**
         * Helper method to print the binary search tree
         */
        public void print(TradingTimeStorage.Node node) {
            if (node == null) {
                return;
            }
            print(node.left);
            System.out.println(node.startTime + " " + node.endTime);
            print(node.right);
        }
    }



}