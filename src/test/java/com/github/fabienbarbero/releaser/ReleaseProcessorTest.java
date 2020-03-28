package com.github.fabienbarbero.releaser;

import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.MergeRequestApi;
import org.gitlab4j.api.TagsApi;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;

/**
 * @author Fabien Barbero
 */
public class ReleaseProcessorTest {

    private DummyContext context;
    private TagsApi tagsApi;
    private ReleaseProcessor processor;

    @BeforeEach
    public void setUp()
            throws Exception {
        context = new DummyContext();

        GitLabApi gitLabApi = mock(GitLabApi.class);
        tagsApi = mock(TagsApi.class);
        when(gitLabApi.getTagsApi()).thenReturn(tagsApi);
        MergeRequestApi mergeRequestApi = mock(MergeRequestApi.class);
        when(gitLabApi.getMergeRequestApi()).thenReturn(mergeRequestApi);
        when(tagsApi.createTag(eq("projectId"), any(), any()))
                .thenAnswer(invocation -> createTag(invocation.getArgument(1)));
        when(mergeRequestApi.createMergeRequest(any(), any())).thenReturn(new MergeRequest());

        processor = new ReleaseProcessor(context, gitLabApi);
    }

    @Test
    public void testReleaseCandidate()
            throws Exception {
        context.setBuildBranch("release-1.0");

        // First RC
        when(tagsApi.getTagsStream("projectId", Constants.TagOrderBy.UPDATED, Constants.SortOrder.DESC, "^test-1.0-RC"))
                .thenReturn(Stream.empty());
        processor.run();
        verify(tagsApi, times(1)).createTag("projectId", "test-1.0-RC1", "release-1.0");
        verify(tagsApi, times(0)).createTag("projectId", "test-1.0-RC2", "release-1.0");
        verify(tagsApi, times(0)).createTag("projectId", "test-1.0-RC3", "release-1.0");

        // Second RC
        when(tagsApi.getTagsStream("projectId", Constants.TagOrderBy.UPDATED, Constants.SortOrder.DESC, "^test-1.0-RC"))
                .thenReturn(Stream.of(createTag("test-1.0-RC1")));
        processor.run();
        verify(tagsApi, times(1)).createTag("projectId", "test-1.0-RC1", "release-1.0");
        verify(tagsApi, times(1)).createTag("projectId", "test-1.0-RC2", "release-1.0");
        verify(tagsApi, times(0)).createTag("projectId", "test-1.0-RC3", "release-1.0");

        // Third RC
        when(tagsApi.getTagsStream("projectId", Constants.TagOrderBy.UPDATED, Constants.SortOrder.DESC, "^test-1.0-RC"))
                .thenReturn(Stream.of(createTag("test-1.0-RC1"), createTag("test-1.0-RC2")));
        processor.run();
        verify(tagsApi, times(1)).createTag("projectId", "test-1.0-RC1", "release-1.0");
        verify(tagsApi, times(1)).createTag("projectId", "test-1.0-RC2", "release-1.0");
        verify(tagsApi, times(1)).createTag("projectId", "test-1.0-RC3", "release-1.0");
    }

    @Test
    public void testFinalRelease()
            throws Exception {
        context.setBuildBranch("master");
        context.setMergeRequestSourceBranch("release-2.0");

        // Release
        processor.run();
        verify(tagsApi, times(1)).createTag("projectId", "test-2.0", "master");
    }

    @Test
    public void testHotfix()
            throws Exception {
        context.setBuildBranch("master");
        context.setMergeRequestSourceBranch("hotfix-bug");

        // First hotfix
        when(tagsApi.getTagsStream("projectId")).thenReturn(Stream.of(createTag("test-1.0")));
        processor.run();
        verify(tagsApi, times(1)).createTag("projectId", "test-1.0.1", "master");
        verify(tagsApi, times(0)).createTag("projectId", "test-1.0.2", "master");

        // Second hotfix
        when(tagsApi.getTagsStream("projectId")).thenReturn(Stream.of(
                createTag("test-1.0"),
                createTag("test-0.1"),
                createTag("test-0.1.1"),
                createTag("test-1.0.1")));
        processor.run();
        verify(tagsApi, times(1)).createTag("projectId", "test-1.0.1", "master");
        verify(tagsApi, times(1)).createTag("projectId", "test-1.0.2", "master");

        // Reverse tags to test sorting
        when(tagsApi.getTagsStream("projectId")).thenReturn(Stream.of(
                createTag("test-1.0.1"),
                createTag("test-1.0"),
                createTag("test-0.1"),
                createTag("test-0.1.1")));
        processor.run();
        verify(tagsApi, times(1)).createTag("projectId", "test-1.0.1", "master");
        verify(tagsApi, times(2)).createTag("projectId", "test-1.0.2", "master");
    }

    private Tag createTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        return tag;
    }

}
