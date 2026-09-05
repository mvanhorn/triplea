package games.strategy.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.junit.jupiter.api.Test;
import org.triplea.swing.SwingAction;

final class ScrollableTextFieldTest {
  private static final Dimension PRODUCTION_TILE_SIZE = new Dimension(150, 150);

  @Test
  void flatLafKeepsDisabledButtonsVisible() throws InterruptedException {
    SwingAction.invokeAndWait(
        () ->
            withLookAndFeel(
                FlatDarkLaf.class.getName(),
                () -> {
                  final var fixture = createFixture(0, 0);

                  assertButtonsVisible(fixture);
                  assertThat(fixture.buttons()).allMatch(button -> !button.isEnabled());
                }));
  }

  @Test
  void flatLafKeepsButtonBoundsWhenEnabledStateChanges() throws InterruptedException {
    SwingAction.invokeAndWait(
        () ->
            withLookAndFeel(
                FlatDarkLaf.class.getName(),
                () -> {
                  final var fixture = createFixture(0, 1);
                  final var initialBounds =
                      fixture.buttons().stream().map(Component::getBounds).toList();

                  assertButtonsVisible(fixture);
                  assertThat(fixture.increment().isEnabled()).isTrue();
                  assertThat(fixture.maximum().isEnabled()).isTrue();
                  assertThat(fixture.decrement().isEnabled()).isFalse();
                  assertThat(fixture.minimum().isEnabled()).isFalse();

                  fixture.increment().doClick();
                  layoutRecursively(fixture.tile());

                  assertThat(fixture.field().getValue()).isEqualTo(1);
                  assertThat(fixture.buttons().stream().map(Component::getBounds).toList())
                      .containsExactlyElementsOf(initialBounds);
                  assertButtonsVisible(fixture);
                  assertThat(fixture.increment().isEnabled()).isFalse();
                  assertThat(fixture.maximum().isEnabled()).isFalse();
                  assertThat(fixture.decrement().isEnabled()).isTrue();
                  assertThat(fixture.minimum().isEnabled()).isTrue();
                }));
  }

  @Test
  void jdkLookAndFeelKeepsButtonsVisibleAndActionsWorking() throws InterruptedException {
    SwingAction.invokeAndWait(
        () ->
            withLookAndFeel(
                UIManager.getCrossPlatformLookAndFeelClassName(),
                () -> {
                  final var fixture = createFixture(0, 1);

                  assertButtonsVisible(fixture);

                  fixture.increment().doClick();
                  assertThat(fixture.field().getValue()).isEqualTo(1);
                  fixture.minimum().doClick();
                  assertThat(fixture.field().getValue()).isZero();
                  fixture.maximum().doClick();
                  assertThat(fixture.field().getValue()).isEqualTo(1);
                  fixture.decrement().doClick();
                  assertThat(fixture.field().getValue()).isZero();

                  layoutRecursively(fixture.tile());
                  assertButtonsVisible(fixture);
                }));
  }

  private static Fixture createFixture(final int min, final int max) {
    final var field = new ScrollableTextField(min, max);
    final var tile = new JPanel(new GridBagLayout());
    tile.add(
        field,
        new GridBagConstraints(
            0,
            3,
            1,
            1,
            0,
            0,
            GridBagConstraints.SOUTH,
            GridBagConstraints.NONE,
            new Insets(5, 5, 5, 5),
            0,
            0));
    tile.setSize(PRODUCTION_TILE_SIZE);
    layoutRecursively(tile);

    final var buttonColumns =
        Arrays.stream(field.getComponents()).filter(JPanel.class::isInstance).toList();
    final var increment = (JButton) ((JPanel) buttonColumns.get(0)).getComponent(0);
    final var decrement = (JButton) ((JPanel) buttonColumns.get(0)).getComponent(1);
    final var maximum = (JButton) ((JPanel) buttonColumns.get(1)).getComponent(0);
    final var minimum = (JButton) ((JPanel) buttonColumns.get(1)).getComponent(1);
    return new Fixture(tile, field, increment, decrement, maximum, minimum);
  }

  private static void assertButtonsVisible(final Fixture fixture) {
    final var visibleArea = new Rectangle(PRODUCTION_TILE_SIZE);
    final var buttonSize = fixture.increment().getPreferredSize();
    assertThat(fixture.buttons())
        .allSatisfy(
            button -> {
              final var boundsInTile =
                  SwingUtilities.convertRectangle(
                      button.getParent(), button.getBounds(), fixture.tile());
              assertThat(boundsInTile.width).isPositive();
              assertThat(boundsInTile.height).isPositive();
              assertThat(visibleArea.contains(boundsInTile)).isTrue();
              assertThat(button.getMinimumSize()).isEqualTo(buttonSize);
              assertThat(button.getPreferredSize()).isEqualTo(buttonSize);
              assertThat(button.getMaximumSize()).isEqualTo(buttonSize);
              final var insets = button.getInsets();
              assertThat(button.getIcon().getIconWidth())
                  .isLessThanOrEqualTo(button.getWidth() - insets.left - insets.right);
              assertThat(button.getIcon().getIconHeight())
                  .isLessThanOrEqualTo(button.getHeight() - insets.top - insets.bottom);
            });
  }

  private static void layoutRecursively(final Container container) {
    container.doLayout();
    Arrays.stream(container.getComponents())
        .filter(Container.class::isInstance)
        .map(Container.class::cast)
        .forEach(ScrollableTextFieldTest::layoutRecursively);
  }

  private static void withLookAndFeel(final String lookAndFeelClassName, final Runnable test) {
    final var originalLookAndFeel = UIManager.getLookAndFeel();
    try {
      UIManager.setLookAndFeel(lookAndFeelClassName);
      test.run();
    } catch (final ReflectiveOperationException | UnsupportedLookAndFeelException e) {
      throw new AssertionError(e);
    } finally {
      try {
        UIManager.setLookAndFeel(originalLookAndFeel);
      } catch (final UnsupportedLookAndFeelException e) {
        throw new AssertionError(e);
      }
    }
  }

  private record Fixture(
      JPanel tile,
      ScrollableTextField field,
      JButton increment,
      JButton decrement,
      JButton maximum,
      JButton minimum) {
    List<JButton> buttons() {
      return List.of(increment, decrement, maximum, minimum);
    }
  }
}
