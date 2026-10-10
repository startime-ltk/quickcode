package com.litukang.quickcode;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 秒码无障碍服务 v1.4
 *
 * 「一键出码」的自动化执行器：
 *   1. MainActivity 点「一键出码」后写入 pendingPkg（目标 App 包名）并拉起该 App；
 *   2. 本服务监听到目标 App 的窗口变化事件后，在后台线程执行出码流程：
 *      - 渐进等待页面渲染完成（每步随机延迟，降低淘宝/拼多多风控误判概率）
 *      - 按 viewId 优先、文本匹配兜底查找节点
 *      - 自动点击「我的」→「身份码/取件码」
 *      - 找不到节点时明确 Toast 提示，绝不静默失败
 *
 * 安全说明：本服务只在用户主动点击「一键出码」按钮、且 pendingPkg 被设置时才工作，
 * 不会在后台偷偷点击任何东西。
 */
public class QuickCodeAccessibilityService extends AccessibilityService {

    /** 由 MainActivity 写入：当前要自动出码的目标包名，null 表示不工作。 */
    public static volatile String pendingPkg = null;

    /** 任务号：每次点「一键出码」递增，用于区分新旧任务、防重复执行。 */
    public static volatile int pendingTaskId = 0;

    private static final Random RANDOM = new Random();

    /** 后台单线程执行任务，避免阻塞无障碍回调线程。 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** 最近已执行的任务号（防同一任务被多次窗口事件重复触发）。 */
    private volatile int executedTaskId = -1;

    /** 是否正在执行任务（防并发重入）。 */
    private volatile boolean taskRunning = false;

    // ------------------------------------------------------------------
    // 节点匹配候选
    // ------------------------------------------------------------------

    /** 「我的」页签候选 viewId（不同 App/版本有差异，文本匹配做兜底）。 */
    private static final String[] TAB_MY_VIEW_IDS = {
            "com.taobao.taobao:id/tv_main_bottom_tab",
            "com.taobao.taobao:id/tab_item",
            "com.xunmeng.pinduoduo:id/tab_my",
            "com.xunmeng.pinduoduo:id/bottom_bar",
            "com.cainiao.wireless:id/tab_my"
    };

    /** 「我的」页签候选文本。 */
    private static final String[] TAB_MY_TEXTS = {
            "我的淘宝", "我的", "个人中心"
    };

    /** 「身份码/取件码」候选 viewId。 */
    private static final String[] CODE_VIEW_IDS = {
            "com.xunmeng.pinduoduo:id/identity_code",
            "com.taobao.taobao:id/identity_code",
            "com.cainiao.wireless:id/identity_code",
            "com.cainiao.wireless:id/pickup_code"
    };

    /** 「身份码/取件码」候选文本。 */
    private static final String[] CODE_TEXTS = {
            "身份码", "取件码", "取件", "身份二维码", "驿站取件码"
    };

    // ------------------------------------------------------------------
    // 对外接口（MainActivity 调用）
    // ------------------------------------------------------------------

    /** 判断秒码的无障碍服务是否已在系统设置中开启。 */
    public static boolean isEnabled(Context ctx) {
        try {
            android.view.accessibility.AccessibilityManager am =
                    (android.view.accessibility.AccessibilityManager)
                            ctx.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am == null) {
                return false;
            }
            List<AccessibilityServiceInfo> list = am.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            if (list == null) {
                return false;
            }
            for (AccessibilityServiceInfo info : list) {
                if (info != null && info.getResolveInfo() != null
                        && info.getResolveInfo().serviceInfo != null
                        && ctx.getPackageName().equals(info.getResolveInfo().serviceInfo.packageName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** 请求执行一次「一键出码」：记录目标包名并递增任务号。 */
    public static void requestTask(String pkg) {
        pendingPkg = pkg;
        pendingTaskId++;
    }

    // ------------------------------------------------------------------
    // AccessibilityService 生命周期
    // ------------------------------------------------------------------

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (pendingPkg == null || taskRunning) {
            return;
        }
        if (event.getPackageName() == null) {
            return;
        }
        if (!pendingPkg.equals(event.getPackageName().toString())) {
            return;
        }
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }
        int taskId = pendingTaskId;
        if (taskId == executedTaskId) {
            return;
        }
        executedTaskId = taskId;
        taskRunning = true;
        executor.execute(() -> {
            try {
                runTask(taskId);
            } catch (Exception ignored) {
                // 任何异常都不允许静默失败：兜底提示
                toast("一键出码执行出错，请重试");
            } finally {
                taskRunning = false;
            }
        });
    }

    @Override
    public void onInterrupt() {
        // 服务被系统中断，无需特殊处理
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    // ------------------------------------------------------------------
    // 出码流程
    // ------------------------------------------------------------------

    private void runTask(int taskId) {
        if (taskId != pendingTaskId) {
            return;
        }
        toast("正在自动出码，请勿操作手机");

        // 阶段 1：直接找「身份码/取件码」（菜鸟等 App 首屏就有，无需进「我的」）
        AccessibilityNodeInfo code = waitForNode(CODE_VIEW_IDS, CODE_TEXTS, 6);
        if (code != null) {
            if (clickNode(code)) {
                toast("已点击「身份码/取件码」，请查看页面");
            } else {
                toast("已找到「身份码/取件码」，但点击失败，请手动点击");
            }
            return;
        }

        // 阶段 2：找「我的」页签并点击
        AccessibilityNodeInfo tab = waitForNode(TAB_MY_VIEW_IDS, TAB_MY_TEXTS, 12);
        if (tab == null) {
            toast("未找到「我的」入口，页面可能未加载完成或布局已变化，请手动操作");
            return;
        }
        if (!clickNode(tab)) {
            toast("已找到「我的」但点击失败，请手动点击");
            return;
        }

        // 阶段 3：进入「我的」页后，渐进等待「身份码/取件码」出现
        sleepRandom(1200, 2200);
        AccessibilityNodeInfo code2 = waitForNode(CODE_VIEW_IDS, CODE_TEXTS, 12);
        if (code2 == null) {
            toast("已进入「我的」页，但未找到「身份码/取件码」入口，请手动查看");
            return;
        }
        if (clickNode(code2)) {
            toast("已点击「身份码/取件码」，请查看页面");
        } else {
            toast("已找到「身份码/取件码」，但点击失败，请手动点击");
        }
    }

    /**
     * 渐进等待目标节点出现：
     * 每次取当前活动窗口根节点做匹配，失败后随机延迟并进入下一轮，
     * 延迟时间随轮次递增（页面越晚渲染完成，越要多等一会）。
     */
    private AccessibilityNodeInfo waitForNode(String[] viewIds, String[] texts, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                AccessibilityNodeInfo n = findNode(root, viewIds, texts);
                if (n != null) {
                    return n;
                }
            }
            // 随机延迟：600-1400ms 起步，每轮 +150-200ms
            sleepRandom(600 + i * 150, 1400 + i * 200);
        }
        return null;
    }

    /** 查找节点：viewId 优先 → findAccessibilityNodeInfosByText 兜底 → 全树扫描兜底。 */
    private AccessibilityNodeInfo findNode(AccessibilityNodeInfo root,
                                           String[] viewIds, String[] texts) {
        if (root == null) {
            return null;
        }
        // 1. viewId 优先
        for (String vid : viewIds) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(vid);
            if (nodes != null) {
                for (AccessibilityNodeInfo n : nodes) {
                    if (isVisibleUsable(n)) {
                        return n;
                    }
                }
            }
        }
        // 2. 系统文本匹配（包含匹配，可能返回多个，取第一个可见可用的）
        for (String t : texts) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(t);
            if (nodes != null) {
                for (AccessibilityNodeInfo n : nodes) {
                    if (isVisibleUsable(n)) {
                        return n;
                    }
                }
            }
        }
        // 3. 全树扫描兜底（findAccessibilityNodeInfosByText 偶尔会漏）
        return scanByText(root, texts);
    }

    /** 深度优先全树扫描：找文本包含目标词、且可见可用的节点。 */
    private AccessibilityNodeInfo scanByText(AccessibilityNodeInfo root, String[] texts) {
        if (root == null) {
            return null;
        }
        CharSequence nodeText = root.getText();
        if (nodeText != null) {
            String s = nodeText.toString();
            for (String t : texts) {
                if (s.contains(t) && isVisibleUsable(root)) {
                    return root;
                }
            }
        }
        CharSequence contentDesc = root.getContentDescription();
        if (contentDesc != null) {
            String s = contentDesc.toString();
            for (String t : texts) {
                if (s.contains(t) && isVisibleUsable(root)) {
                    return root;
                }
            }
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo hit = scanByText(child, texts);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    /** 节点是否可见、可用（可点击或父级可点击）。 */
    private boolean isVisibleUsable(AccessibilityNodeInfo n) {
        if (n == null || !n.isVisibleToUser() || !n.isEnabled()) {
            return false;
        }
        if (n.isClickable()) {
            return true;
        }
        AccessibilityNodeInfo p = n.getParent();
        while (p != null) {
            if (p.isClickable() && p.isVisibleToUser()) {
                return true;
            }
            p = p.getParent();
        }
        return true;
    }

    /** 点击节点：优先点自身，自身不可点则沿父链找可点击节点。 */
    private boolean clickNode(AccessibilityNodeInfo n) {
        if (n == null) {
            return false;
        }
        if (n.isClickable() && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
        }
        AccessibilityNodeInfo p = n.getParent();
        while (p != null) {
            if (p.isClickable() && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
            p = p.getParent();
        }
        return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    // ------------------------------------------------------------------
    // 工具方法
    // ------------------------------------------------------------------

    private void sleepRandom(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + RANDOM.nextInt(maxMs - minMs));
        } catch (InterruptedException ignored) {
            // 线程被中断，直接返回
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
