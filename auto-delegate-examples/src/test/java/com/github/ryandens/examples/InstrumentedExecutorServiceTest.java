package com.github.ryandens.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InstrumentedExecutorService} */
final class InstrumentedExecutorServiceTest {

  private InstrumentedExecutorService instrumentedExecutorService;
  private ExecutorService executorServiceMock;

  @BeforeEach
  void beforeEach() {
    // use a mock executor service to avoid actually executing the Runnables/Callables as it is not
    // necessary to test the InstrumentedExecutorService
    executorServiceMock = mock(ExecutorService.class);
    // GIVEN an instrumented ExecutorService
    instrumentedExecutorService = new InstrumentedExecutorService(executorServiceMock);
  }

  @Test
  void testExecute() {
    // WHEN we execute a task
    final Runnable task = () -> System.out.println("bar");
    instrumentedExecutorService.execute(task);
    // VERIFY the task is delegated
    verify(executorServiceMock).execute(same(task));
    // VERIFY the count is correct
    assertEquals(1, instrumentedExecutorService.getNumJobs());
  }
}
