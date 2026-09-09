package com.rahul.reconciliation;
import com.rahul.entity.FileStatus; import com.rahul.repository.FileMetadataRepository; import com.rahul.storage.ObjectStorage; import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j; import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component; import java.util.UUID;
@Component @RequiredArgsConstructor @Slf4j @ConditionalOnProperty(name="reconciliation.enabled",havingValue="true",matchIfMissing=false)
public class FileReconciliationJob{
 private final FileMetadataRepository files; private final ObjectStorage storage;
 @Scheduled(fixedDelayString="${reconciliation.delay-ms:60000}") public void reconcile(){files.findAll().stream().filter(f->f.getStatus()==FileStatus.DELETING).forEach(f->{try{if(f.getObjectKey()==null||!storage.exists(f.getObjectKey()))log.warn("Deletion reconciliation: file {} original object missing",f.getId());}catch(Exception e){log.warn("Reconciliation failed for {}",f.getId(),e);}});}
}
