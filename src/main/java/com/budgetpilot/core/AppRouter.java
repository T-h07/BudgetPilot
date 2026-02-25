package com.budgetpilot.core;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class AppRouter {
    private final Map<PageId, Supplier<Node>> routes = new EnumMap<>(PageId.class);
    private final ReadOnlyObjectWrapper<PageId> currentPageId = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<Node> currentPageNode = new ReadOnlyObjectWrapper<>();

    public void register(PageId pageId, Supplier<Node> pageFactory) {
        Objects.requireNonNull(pageId, "pageId must not be null");
        Objects.requireNonNull(pageFactory, "pageFactory must not be null");

        if (routes.containsKey(pageId)) {
            throw new IllegalStateException("Page is already registered: " + pageId);
        }
        routes.put(pageId, pageFactory);
    }

    public Node navigate(PageId pageId) {
        Objects.requireNonNull(pageId, "pageId must not be null");
        Supplier<Node> pageFactory = routes.get(pageId);

        if (pageFactory == null) {
            throw new IllegalStateException(
                    "No route registered for page " + pageId + ". Registered routes: " + routes.keySet()
            );
        }

        Node pageNode = pageFactory.get();
        if (pageNode == null) {
            throw new IllegalStateException("Route for page " + pageId + " returned null");
        }

        currentPageId.set(pageId);
        currentPageNode.set(pageNode);
        return pageNode;
    }

    public boolean isRegistered(PageId pageId) {
        return routes.containsKey(pageId);
    }

    public PageId getCurrentPageId() {
        return currentPageId.get();
    }

    public ReadOnlyObjectProperty<PageId> currentPageIdProperty() {
        return currentPageId.getReadOnlyProperty();
    }

    public Node getCurrentPageNode() {
        return currentPageNode.get();
    }

    public ReadOnlyObjectProperty<Node> currentPageNodeProperty() {
        return currentPageNode.getReadOnlyProperty();
    }
}
