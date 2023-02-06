package moonrise.pjt1.debate.controller;

import lombok.RequiredArgsConstructor;
import moonrise.pjt1.debate.dto.DebateCreateDto;
import moonrise.pjt1.debate.service.DebateService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debate")
@RequiredArgsConstructor
public class DebateController {
    private final DebateService debateService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(value = "movieId") Long movieId,
                                                    @RequestParam(value = "page", defaultValue = "0") int page){
        PageRequest pageable = PageRequest.of(page, 6, Sort.by("id").descending());
        Map<String, Object> result = debateService.listDebate(movieId, pageable);
        return new ResponseEntity<Map<String, Object>>(result, HttpStatus.ACCEPTED);
    }

    @PostMapping("/create") // 파티 생성
    public ResponseEntity<Map<String, Object>> createParty(@RequestBody DebateCreateDto debateCreateDto){
        Map<String, Object> result = new HashMap<>();
        Long debateId = debateService.createParty(debateCreateDto);
        result.put("debateId",debateId);
        return new ResponseEntity<Map<String, Object>>(result, HttpStatus.ACCEPTED);
    }

    @GetMapping("/read/{debateId}") // 파티 상세보기
    public ResponseEntity<Map<String, Object>> read(@PathVariable Long debateId){
        Map<String, Object> result = debateService.readDebate(debateId);
        return new ResponseEntity<Map<String, Object>>(result, HttpStatus.ACCEPTED);
    }
}
